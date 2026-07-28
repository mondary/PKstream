<?php
declare(strict_types=1);

const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36';

header('Access-Control-Allow-Origin: *');
header('X-Content-Type-Options: nosniff');

function http_get(string $url, string $referer): ?string {
    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_USERAGENT => UA,
        CURLOPT_REFERER => $referer,
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_SSL_VERIFYPEER => false,
        CURLOPT_FRESH_CONNECT => true,
        CURLOPT_HTTPHEADER => ['Accept: */*'],
    ]);
    $result = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
    $error = curl_error($ch);
    curl_close($ch);
    if ($status >= 200 && $status < 300 && $result) return $result;
    error_log("proxy http_get FAIL status=$status url=" . substr($url, 0, 80) . " err=$error");
    return null;
}

function unpack_packer(string $html): ?string {
    if (!preg_match("/}\('((?:\\\\.|[^'])*)',(\d+),\d+,'((?:\\\\.|[^'])*)'\.split\('\\|'\)/s", $html, $m)) return null;
    $payload = stripcslashes($m[1]);
    $radix = (int)$m[2];
    $words = explode('|', stripcslashes($m[3]));
    return preg_replace_callback('/\b[0-9a-z]+\b/i', static function (array $tok) use ($radix, $words): string {
        $idx = (int)base_convert(strtolower($tok[0]), $radix, 10);
        $word = $words[$idx] ?? '';
        return $word !== '' ? $word : $tok[0];
    }, $payload);
}

function resolve_embed(string $embedUrl): ?array {
    $html = http_get($embedUrl, $embedUrl);
    if (!$html) return null;
    $decoded = unpack_packer($html);
    if (!$decoded) $decoded = $html;
    if (!preg_match("~https?://[^\s\"']+\.m3u8[^\s\"']*~i", $decoded, $m)) return null;
    $url = html_entity_decode(str_replace('\\/', '/', $m[0]));
    $url = preg_replace('/[.,\s]+$/', '', $url);
    $origin = (parse_url($embedUrl, PHP_URL_SCHEME) ?: 'https') . '://' . parse_url($embedUrl, PHP_URL_HOST);
    return ['url' => $url, 'referer' => $origin . '/'];
}

function self_url(string $target, string $ref): string {
    return 'https://mondary.design/pk/stream/stremio/proxy.php?url=' . base64_encode($target) . '&ref=' . base64_encode($ref);
}

function to_absolute(string $base, string $relative): string {
    if (parse_url($relative, PHP_URL_SCHEME)) return $relative;
    $p = parse_url($base);
    $scheme = $p['scheme'] ?? 'https';
    $host = $p['host'] ?? '';
    $port = isset($p['port']) ? ':' . $p['port'] : '';
    $path = $p['path'] ?? '/';
    if (isset($relative[0]) && $relative[0] === '/') return "$scheme://$host$port$relative";
    $dir = dirname($path);
    if ($dir === '.') $dir = '/';
    if ($dir === '\\') $dir = '/';
    return "$scheme://$host$port$dir/$relative";
}

// ── Route 1: resolve embed → fetch m3u8 → rewrite ──
$embed = $_GET['embed'] ?? '';
if ($embed) {
    $embedUrl = base64_decode($embed);
    if (!$embedUrl || !filter_var($embedUrl, FILTER_VALIDATE_URL)) {
        http_response_code(400);
        exit('Invalid embed URL');
    }

    $resolved = resolve_embed($embedUrl);
    if (!$resolved) {
        http_response_code(502);
        exit('Cannot resolve stream');
    }

    $m3u8 = http_get($resolved['url'], $resolved['referer']);
    if ($m3u8 === null) {
        http_response_code(502);
        exit('Cannot fetch manifest');
    }

    header('Content-Type: application/vnd.apple.mpegurl');
    $ref = $resolved['referer'];
    $baseUrl = $resolved['url'];
    $lines = explode("\n", $m3u8);
    $out = [];
    foreach ($lines as $line) {
        $line = trim($line);
        if ($line === '') continue;
        if ($line[0] === '#') {
            if (str_starts_with($line, '#EXT-X-KEY:') || str_starts_with($line, '#EXT-X-MEDIA:')) {
                $line = preg_replace_callback('/URI="([^"]+)"/', function ($m) use ($baseUrl, $ref) {
                    $abs = to_absolute($baseUrl, $m[1]);
                    return 'URI="' . self_url($abs, $ref) . '"';
                }, $line);
            }
            $out[] = $line;
        } else {
            $abs = to_absolute($baseUrl, $line);
            $out[] = self_url($abs, $ref);
        }
    }
    echo implode("\n", $out);
    exit;
}

// ── Route 2: pass-through segment/playlist with Referer ──
$target = base64_decode($_GET['url'] ?? '');
$ref = base64_decode($_GET['ref'] ?? '');
if (!$target || !filter_var($target, FILTER_VALIDATE_URL)) {
    http_response_code(400);
    exit('Invalid URL');
}

$origin = (parse_url($target, PHP_URL_SCHEME) ?: 'https') . '://' . (parse_url($target, PHP_URL_HOST) ?: '');
$referer = $ref ?: $origin . '/';

$content = http_get($target, $referer);
if ($content === null) {
    http_response_code(502);
    exit('Upstream error');
}

if (str_contains($content, '#EXTM3U')) {
    header('Content-Type: application/vnd.apple.mpegurl');
    $lines = explode("\n", $content);
    $out = [];
    foreach ($lines as $line) {
        $line = trim($line);
        if ($line === '') continue;
        if ($line[0] === '#') {
            if (str_starts_with($line, '#EXT-X-KEY:') || str_starts_with($line, '#EXT-X-MEDIA:')) {
                $line = preg_replace_callback('/URI="([^"]+)"/', function ($m) use ($target, $referer) {
                    $abs = to_absolute($target, $m[1]);
                    return 'URI="' . self_url($abs, $referer) . '"';
                }, $line);
            }
            $out[] = $line;
        } else {
            $abs = to_absolute($target, $line);
            $out[] = self_url($abs, $referer);
        }
    }
    echo implode("\n", $out);
} else {
    $ext = pathinfo(parse_url($target, PHP_URL_PATH), PATHINFO_EXTENSION);
    $ct = match($ext) {
        'ts' => 'video/mp2t',
        'mp4' => 'video/mp4',
        'm4s' => 'video/iso.segment',
        default => 'application/octet-stream',
    };
    header("Content-Type: $ct");
    echo $content;
}
