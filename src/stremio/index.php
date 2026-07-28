<?php
declare(strict_types=1);
// PKSTREAM-v5

const BASE = 'https://fs16.lol';
const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36';

header('Access-Control-Allow-Origin: *');
header('X-Content-Type-Options: nosniff');

function jsonResponse(array $body): never {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($body, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

function request(string $url, ?string $postBody = null): ?string {
    $curl = curl_init($url);
    curl_setopt_array($curl, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_USERAGENT => UA,
        CURLOPT_REFERER => BASE . '/',
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_TIMEOUT => 20,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_SSL_VERIFYPEER => false,
    ]);
    if ($postBody !== null) {
        curl_setopt($curl, CURLOPT_POST, true);
        curl_setopt($curl, CURLOPT_POSTFIELDS, $postBody);
        curl_setopt($curl, CURLOPT_HTTPHEADER, ['Content-Type: application/x-www-form-urlencoded']);
    }
    $result = curl_exec($curl);
    curl_close($curl);
    return is_string($result) ? $result : null;
}

function normalize(string $value): string {
    $value = mb_strtolower($value, 'UTF-8');
    $value = iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', $value) ?: $value;
    return preg_replace('/[^a-z0-9]/', '', $value) ?: '';
}

function contentMeta(string $type, string $imdbId): ?array {
    $metaType = $type === 'series' ? 'series' : 'movie';
    $json = request("https://v3-cinemeta.strem.io/meta/$metaType/$imdbId.json");
    $data = $json ? json_decode($json, true) : null;
    return is_array($data) && is_array($data['meta'] ?? null) ? $data['meta'] : null;
}

function contentTitle(string $type, string $imdbId): ?string {
    return contentMeta($type, $imdbId)['name'] ?? null;
}

function catalog(string $type): array {
    $json = request("https://v3-cinemeta.strem.io/catalog/$type/top.json");
    $data = $json ? json_decode($json, true) : null;
    return is_array($data) && is_array($data['metas'] ?? null) ? $data['metas'] : [];
}

function searchFs16(string $title): array {
    $html = request(BASE . '/index.php', 'do=search&subaction=search&story=' . rawurlencode($title));
    if (!$html) return [];
    preg_match_all('/href="\/index\.php\?newsid=(\d+)"[^>]*alt="([^"]*)"/', $html, $matches, PREG_SET_ORDER);
    return array_map(static fn(array $match): array => ['id' => $match[1], 'title' => trim(html_entity_decode($match[2]))], $matches);
}

function bestMatch(array $results, string $title, ?int $season): ?array {
    $target = normalize($title);
    foreach ($results as $result) {
        $candidate = normalize($result['title']);
        if ($season !== null && str_contains($candidate, 'saison' . $season) && str_contains($candidate, $target)) return $result;
        if ($season === null && $candidate === $target) return $result;
    }
    foreach ($results as $result) {
        $candidate = normalize($result['title']);
        if (str_contains($candidate, $target) || str_contains($target, $candidate)) return $result;
    }
    return $results[0] ?? null;
}

function unpackPacker(string $html): ?string {
    if (!preg_match("/}\('((?:\\\\.|[^'])*)',(\d+),\d+,'((?:\\\\.|[^'])*)'\.split\('\\|'\)/s", $html, $match)) return null;
    $payload = stripcslashes($match[1]);
    $radix = (int)$match[2];
    $words = explode('|', stripcslashes($match[3]));
    return preg_replace_callback('/\b[0-9a-z]+\b/i', static function (array $token) use ($radix, $words): string {
        $index = (int)base_convert(strtolower($token[0]), $radix, 10);
        $word = $words[$index] ?? '';
        return $word !== '' ? $word : $token[0];
    }, $payload);
}

function resolveEmbed(string $embedUrl): ?array {
    $decoded = unpackPacker(request($embedUrl) ?? '');
    if (!$decoded) return null;
    if (!preg_match("~https?://[^\\s\"']+\\.(m3u8|mp4)[^\\s\"']*~i", $decoded, $match)) return null;
    $streamUrl = html_entity_decode(str_replace('\\/', '/', $match[0]));
    $streamUrl = preg_replace('/[.,\s]+$/', '', $streamUrl);
    $origin = (parse_url($embedUrl, PHP_URL_SCHEME) ?? 'https') . '://' . parse_url($embedUrl, PHP_URL_HOST);
    return ['url' => $streamUrl, 'referer' => $origin . '/'];
}

function proxyUrl(string $embedUrl): string {
    return 'https://mondary.design/pk/stream/stremio/proxy.php?embed=' . base64_encode($embedUrl);
}

function stream(string $url, string $name): array {
    return ['name' => "French Stream\n$name", 'title' => $name, 'url' => $url];
}

function filmStreams(string $newsId): array {
    $json = request(BASE . '/engine/ajax/film_api.php?id=' . rawurlencode($newsId));
    $players = $json ? (json_decode($json, true)['players'] ?? []) : [];
    $streams = [];
    $seen = [];
    foreach (['vidzy', 'uqload', 'dood', 'voe', 'premium'] as $player) {
        foreach (['default' => '', 'vostfr' => ' VOSTFR', 'vfq' => ' VF', 'vff' => ' VF'] as $version => $label) {
            $url = $players[$player][$version] ?? null;
            if (!is_string($url) || !filter_var($url, FILTER_VALIDATE_URL)) continue;
            if (isset($seen[$url])) continue;
            $seen[$url] = true;
            $proxied = proxyUrl($url);
            $streams[] = stream($proxied, ucfirst($player) . $label);
        }
    }
    return $streams;
}

function seriesStreams(string $newsId, int $season, int $episode): array {
    $json = request(BASE . '/static/series/' . rawurlencode($newsId) . '.js');
    $data = $json ? json_decode($json, true) : null;
    if (!is_array($data)) return [];
    $streams = [];
    $seen = [];
    foreach (['vf' => ' VF', 'vostfr' => ' VOSTFR', 'vo' => ' VO'] as $version => $label) {
        $sources = $data[$version][(string)$episode] ?? [];
        foreach (['vidzy', 'uqload', 'premium', 'voe', 'netu'] as $player) {
            $url = $sources[$player] ?? null;
            if (!is_string($url) || !filter_var($url, FILTER_VALIDATE_URL)) continue;
            if (isset($seen[$url])) continue;
            $seen[$url] = true;
            $proxied = proxyUrl($url);
            $streams[] = stream($proxied, "S{$season}E{$episode} " . ucfirst($player) . $label);
        }
    }
    return $streams;
}

$path = trim(parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH) ?: '', '/');
$segments = explode('/', $path);
$metaIndexes = array_keys($segments, 'meta', true);
$metaIndex = end($metaIndexes);
if ($metaIndex !== false && isset($segments[$metaIndex + 2])) {
    $metaType = $segments[$metaIndex + 1];
    $metaId = preg_replace('/\.json$/', '', $segments[$metaIndex + 2]);
    if (in_array($metaType, ['movie', 'series'], true) && preg_match('/^tt\d+$/', $metaId)) {
        $meta = contentMeta($metaType, $metaId);
        jsonResponse(['meta' => $meta]);
    }
    jsonResponse(['meta' => null]);
}

$catalogIndexes = array_keys($segments, 'catalog', true);
$catalogIndex = end($catalogIndexes);
if ($catalogIndex !== false && isset($segments[$catalogIndex + 2])) {
    $catalogType = $segments[$catalogIndex + 1];
    $catalogId = preg_replace('/\.json$/', '', $segments[$catalogIndex + 2]);
    if (($catalogType === 'movie' && $catalogId === 'fs16-movies') || ($catalogType === 'series' && $catalogId === 'fs16-series')) {
        jsonResponse(['metas' => catalog($catalogType)]);
    }
    jsonResponse(['metas' => []]);
}

$streamIndexes = array_keys($segments, 'stream', true);
$streamIndex = end($streamIndexes);
if ($streamIndex === false || !isset($segments[$streamIndex + 2])) jsonResponse(['streams' => []]);

$type = $segments[$streamIndex + 1];
$id = preg_replace('/\.json$/', '', $segments[$streamIndex + 2]);
if (!in_array($type, ['movie', 'series'], true) || !preg_match('/^(tt\d+)(?::(\d+):(\d+))?$/', $id, $parts)) jsonResponse(['streams' => []]);

$isSeries = $type === 'series';
$season = $isSeries ? (int)($parts[2] ?? 0) : null;
$episode = $isSeries ? (int)($parts[3] ?? 0) : null;
if ($isSeries && (!$season || !$episode)) jsonResponse(['streams' => []]);

 $title = contentTitle($type, $parts[1]);
 $results = $title ? searchFs16($title) : [];
 $entry = $title ? bestMatch($results, $title, $season) : null;
 if (!$entry) jsonResponse(['streams' => []]);
 $streams = $isSeries ? seriesStreams($entry['id'], $season, $episode) : filmStreams($entry['id']);
 jsonResponse(['streams' => $streams]);
