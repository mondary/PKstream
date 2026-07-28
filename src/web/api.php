<?php
/**
 * French Stream - Test Scraper
 * Proxy pour tester le scraper fs16.lol
 */
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: SAMEORIGIN');

$BASE = 'https://fs16.lol';
$UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36';

function proxy_get($url) {
    global $BASE, $UA;
    $ch = curl_init($BASE . $url);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_USERAGENT => $UA,
        CURLOPT_REFERER => $BASE . '/',
        CURLOPT_TIMEOUT => 10,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_SSL_VERIFYPEER => false,
    ]);
    $r = curl_exec($ch);
    curl_close($ch);
    return $r;
}

function proxy_post($path, $body) {
    global $BASE, $UA;
    $ch = curl_init($BASE . $path);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_USERAGENT => $UA,
        CURLOPT_REFERER => $BASE . '/',
        CURLOPT_TIMEOUT => 10,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_SSL_VERIFYPEER => false,
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => $body,
        CURLOPT_HTTPHEADER => ['Content-Type: application/x-www-form-urlencoded'],
    ]);
    $r = curl_exec($ch);
    curl_close($ch);
    return $r;
}

// API endpoints
if (isset($_GET['api'])) {
    header('Content-Type: application/json; charset=utf-8');
    $api = $_GET['api'];

    if ($api === 'featured') {
        // Films from /films/
        $html = proxy_get('/films/');
        preg_match_all('/href="\/index\.php\?newsid=(\d+)"[^>]*alt="([^"]*)"[^>]*>.*?<img[^>]+src="([^"]*)"/s', $html, $m);
        $films = [];
        for ($i = 0; $i < count($m[1]); $i++) {
            $films[] = [
                'newsid' => $m[1][$i],
                'title' => trim($m[2][$i]),
                'poster' => html_entity_decode($m[3][$i]),
            ];
        }

        // Series from /s-tv/
        $htmlS = proxy_get('/s-tv/');
        preg_match_all('/href="\/index\.php\?newsid=(\d+)"[^>]*alt="([^"]*)"[^>]*>.*?<img[^>]+src="([^"]*)"/s', $htmlS, $ms);
        $series = [];
        for ($i = 0; $i < count($ms[1]); $i++) {
            $series[] = [
                'newsid' => $ms[1][$i],
                'title' => trim($ms[2][$i]),
                'poster' => html_entity_decode($ms[3][$i]),
            ];
        }

        $films = array_slice($films, 0, 18);
        $series = array_slice($series, 0, 18);
        $hero = $films[0] ?? null;
        if ($hero) {
            $heroData = json_decode(proxy_get('/engine/ajax/film_api.php?id=' . $hero['newsid']), true);
            $hero['backdrop'] = $heroData['meta']['affiche2'] ?? $hero['poster'];
        }
        echo json_encode(['hero' => $hero, 'films' => $films, 'series' => $series]);
        exit;
    }

    if ($api === 'search') {
        $q = $_GET['q'] ?? '';
        $body = 'do=search&subaction=search&story=' . urlencode($q);
        $html = proxy_post('/index.php', $body);
        // Detect rate-limit page from fs16
        if (stripos($html, 'RALENTIS UN PEU') !== false || stripos($html, 'Ralentis un peu') !== false) {
            echo json_encode(['results' => [], 'rateLimited' => true]);
            exit;
        }
        preg_match_all('/href="\/index\.php\?newsid=(\d+)"[^>]*alt="([^"]*)"[^>]*>.*?<img[^>]+src="([^"]*)"/s', $html, $m);
        $results = [];
        for ($i = 0; $i < count($m[1]); $i++) {
            $results[] = [
                'newsid' => $m[1][$i],
                'title' => trim($m[2][$i]),
                'poster' => html_entity_decode($m[3][$i]),
            ];
        }
        echo json_encode(['results' => $results]);
        exit;
    }

    if ($api === 'details') {
        $nid = $_GET['id'] ?? '';
        $filmData = json_decode(proxy_get('/engine/ajax/film_api.php?id=' . $nid), true);
        $html = proxy_get('/index.php?newsid=' . $nid);

        $desc = '';
        if (preg_match('/<meta name="description" content="([^"]*)"/s', $html, $dm)) {
            $desc = trim(html_entity_decode($dm[1]));
        } elseif (preg_match('/<meta property="og:description" content="([^"]*)"/s', $html, $dm)) {
            $desc = trim(html_entity_decode($dm[1]));
        } elseif (preg_match('/id="desc-' . preg_quote($nid, '/') . '"[^>]*>(.*?)<\/span>/s', $html, $dm)) {
            $desc = trim(html_entity_decode(strip_tags($dm[1])));
        }
        $trailer = '';
        if (preg_match('/id="trailer-' . preg_quote($nid, '/') . '"[^>]*>(.*?)<\/span>/s', $html, $tm)) {
            $trailer = trim($tm[1]);
        }

        echo json_encode([
            'newsid' => $nid,
            'backdrop' => $filmData['meta']['affiche2'] ?? '',
            'poster' => $filmData['meta']['affiche'] ?? '',
            'description' => $desc,
            'trailer' => $trailer,
        ]);
        exit;
    }

    if ($api === 'episodes') {
        $nid = $_GET['id'] ?? '';
        $data = json_decode(proxy_get('/static/series/' . $nid . '.js'), true);
        $info = is_array($data) ? ($data['info'] ?? []) : [];
        $episodes = [];
        foreach ($info as $number => $episode) {
            $episodes[] = [
                'number' => (int)$number,
                'title' => $episode['title'] ?? ('Episode ' . $number),
                'synopsis' => $episode['synopsis'] ?? '',
                'poster' => $episode['poster'] ?? '',
            ];
        }
        echo json_encode(['episodes' => $episodes]);
        exit;
    }

    if ($api === 'seasons') {
        $title = $_GET['title'] ?? '';
        $currentNid = $_GET['id'] ?? '';
        $body = 'do=search&subaction=search&story=' . urlencode($title);
        $html = proxy_post('/index.php', $body);
        preg_match_all('/href="\/index\.php\?newsid=(\d+)"[^>]*alt="([^"]*)"/', $html, $m);
        $seasons = [];
        $seen = [];
        for ($i = 0; $i < count($m[1]); $i++) {
            $t = trim(html_entity_decode($m[2][$i]));
            if (preg_match('/^(.+?)\s*-\s*Saison\s*(\d+)/iu', $t, $sm)) {
                $baseName = trim($sm[1]);
                $seasonNum = (int)$sm[2];
                $match = stripos($baseName, $title) !== false || stripos($title, $baseName) !== false;
                if ($match && !isset($seen[$seasonNum])) {
                    $seen[$seasonNum] = true;
                    $seasons[] = ['season' => $seasonNum, 'newsid' => $m[1][$i], 'title' => $t];
                }
            }
        }
        if (!isset($seen[1]) && $currentNid) {
            array_unshift($seasons, ['season' => 1, 'newsid' => $currentNid, 'title' => $title]);
        }
        usort($seasons, fn($a, $b) => $a['season'] <=> $b['season']);
        echo json_encode(['seasons' => $seasons]);
        exit;
    }

    if ($api === 'streams') {
        $nid = $_GET['id'] ?? '';
        $t = $_GET['t'] ?? 'film';

        if ($t === 'tv') {
            $s = $_GET['s'] ?? '1';
            $e = $_GET['e'] ?? '1';
            $data = json_decode(proxy_get('/static/series/' . $nid . '.js'), true);
            $streams = [];
            $vl = ['vf' => ' VF', 'vostfr' => ' VOSTFR', 'vo' => ' VO'];
            foreach (['vf', 'vostfr', 'vo'] as $ver) {
                $ep = $data[$ver][$e] ?? null;
                if (!$ep) continue;
                foreach (['vidzy', 'uqload', 'premium', 'voe', 'netu'] as $p) {
                    if (!empty($ep[$p])) {
                        $streams[] = ['title' => "S{$s}E{$e} - " . ucfirst($p) . ($vl[$ver] ?? ''), 'url' => $ep[$p]];
                    }
                }
            }
            echo json_encode(['streams' => $streams]);
        } else {
            $data = json_decode(proxy_get('/engine/ajax/film_api.php?id=' . $nid), true);
            $streams = [];
            $vl = ['default' => '', 'vostfr' => ' VOSTFR', 'vfq' => ' VF', 'vff' => ' VF'];
            foreach (['vidzy', 'uqload', 'dood', 'voe', 'premium'] as $pn) {
                $pd = $data['players'][$pn] ?? [];
                foreach (['default', 'vostfr', 'vfq', 'vff'] as $vk) {
                    $u = $pd[$vk] ?? $pd['default'] ?? '';
                    if ($u) $streams[] = ['title' => ucfirst($pn) . ($vl[$vk] ?? ''), 'url' => $u];
                }
            }
            echo json_encode(['streams' => $streams]);
        }
        exit;
    }
    exit;
}
?>
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>French Stream - Test Scraper</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:-apple-system,sans-serif;background:#0a0a0a;color:#eee;padding:16px;min-height:100vh}
h1{font-size:20px;margin-bottom:4px;color:#ff8c00}
.sub{color:#666;font-size:12px;margin-bottom:16px}
.sb{display:flex;gap:8px;margin-bottom:12px}
input{flex:1;padding:12px;border:1px solid #333;border-radius:8px;background:#1a1a1a;color:#fff;font-size:16px}
button{padding:12px 20px;border:none;border-radius:8px;background:#ff8c00;color:#000;font-weight:600;font-size:16px;cursor:pointer}
button:disabled{opacity:.5}
.st{color:#888;font-size:13px;margin-bottom:12px;min-height:20px}
.r{background:#1a1a1a;border:1px solid #333;border-radius:10px;padding:14px;margin-bottom:10px}
.rt{font-weight:600;font-size:15px;margin-bottom:4px}
.rid{color:#888;font-size:12px;margin-bottom:8px}
.sn{color:#ff8c00;font-weight:600;font-size:13px}
.su{color:#6cf;word-break:break-all;font-size:11px;margin-top:4px;line-height:1.4}
.s{background:#111;border:1px solid #222;border-radius:6px;padding:10px;margin-bottom:6px}
.pb{display:inline-block;margin-top:8px;padding:8px 16px;background:#ff8c00;color:#000;border-radius:6px;text-decoration:none;font-weight:600;font-size:13px}
.tg{display:flex;gap:8px;margin-bottom:12px}
.tb{padding:8px 16px;border:1px solid #333;border-radius:6px;background:#1a1a1a;color:#888;font-size:14px;cursor:pointer}
.tb.a{border-color:#ff8c00;color:#ff8c00;background:#1a1000}
.se{display:none;gap:8px;margin-bottom:12px}
.se.v{display:flex}
.se input{max-width:80px;text-align:center}
.note{background:#1a1000;border:1px solid #333;border-radius:8px;padding:12px;margin-bottom:16px;font-size:12px;color:#aaa;line-height:1.5}
.note b{color:#ff8c00}
</style>
</head>
<body>
<h1>French Stream - Test Scraper</h1>
<div class="sub">Plugin ARVIO pour fs16.lol</div>

<div class="note">
<b>Test du scraper</b> — Recherche un titre sur fs16.lol, affiche les newsid trouvés, puis charge les sources vidéo (vidzy, uqload, dood, voe).
</div>

<div class="tg">
<div class="tb a" data-type="movie" onclick="ST('movie')">Film</div>
<div class="tb" data-type="tv" onclick="ST('tv')">Série</div>
</div>
<div class="sb">
<input id="q" placeholder="Titre du film ou de la série..." value="Supergirl">
<button onclick="doS()" id="btn">Chercher</button>
</div>
<div class="se" id="se">
<input id="ss" type="number" placeholder="Saison" value="3" min="1">
<input id="ep" type="number" placeholder="Épisode" value="1" min="1">
</div>
<div class="st" id="st"></div>
<div id="res"></div>
<script>
let ct='movie';
function ST(t){ct=t;document.querySelectorAll('.tb').forEach(b=>b.classList.toggle('a',b.dataset.type===t));document.getElementById('se').classList.toggle('v',t==='tv');}
async function doS(){
const q=document.getElementById('q').value.trim();if(!q)return;
const st=document.getElementById('st'),rs=document.getElementById('res'),b=document.getElementById('btn');
b.disabled=true;st.textContent='Recherche en cours...';rs.innerHTML='';
try{
const r=await fetch('?api=search&q='+encodeURIComponent(q));const d=await r.json();
if(!d.results||!d.results.length){st.textContent='Aucun résultat pour "'+q+'"';b.disabled=false;return;}
st.textContent=d.results.length+' résultat(s). Chargement des sources...';
for(const e of d.results.slice(0,3)){
const div=document.createElement('div');div.className='r';
div.innerHTML='<div class="rt">'+e.title.replace(/</g,'&lt;')+'</div><div class="rid">newsid: '+e.newsid+'</div><div id="s'+e.newsid+'"><em style="color:#666">Chargement...</em></div>';
rs.appendChild(div);
let u;if(ct==='tv'){const ss=document.getElementById('ss').value||'1',ep=document.getElementById('ep').value||'1';u='?api=streams&id='+e.newsid+'&t=tv&s='+ss+'&e='+ep;}else{u='?api=streams&id='+e.newsid+'&t=film';}
try{const sr=await fetch(u);const sd=await sr.json();const c=document.getElementById('s'+e.newsid);
if(!sd.streams||!sd.streams.length){c.innerHTML='<em style="color:#666">Aucune source disponible</em>';}
else{c.innerHTML='<div>'+sd.streams.map(s=>'<div class="s"><div class="sn">'+s.title.replace(/</g,'&lt;')+'</div><div class="su">'+s.url.replace(/</g,'&lt;')+'</div><a class="pb" href="'+s.url.replace(/"/g,'%22')+'" target="_blank" rel="noopener">Ouvrir le lecteur</a></div>').join('')+'</div>';}
}catch(e){document.getElementById('s'+e.newsid).innerHTML='<em style="color:red">Erreur: '+e.message+'</em>';}
}st.textContent='';}catch(e){st.textContent='Erreur: '+e.message;}
b.disabled=false;}
ST('movie');
</script>
</body>
</html>
