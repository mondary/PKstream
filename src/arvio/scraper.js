var BASE = 'https://fs16.lol';
var UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36';

var HEADERS = {
    'User-Agent': UA,
    'Referer': BASE + '/',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
};

function normalize(s) {
    return (s || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]/g, '');
}

async function tmdbLookup(tmdbId, mediaType) {
    var apiKey = globalThis.TMDB_API_KEY;
    if (!apiKey) return null;
    var path = mediaType === 'tv' ? 'tv' : 'movie';
    var url = 'https://api.themoviedb.org/3/' + path + '/' + tmdbId + '?api_key=' + apiKey + '&language=fr-FR';
    try {
        var res = await fetch(url, { headers: { 'Accept': 'application/json' } });
        if (!res.ok) return null;
        return await res.json();
    } catch (e) {
        return null;
    }
}

async function searchFs16(query) {
    var url = BASE + '/index.php';
    var body = 'do=search&subaction=search&story=' + encodeURIComponent(query);
    try {
        var res = await fetch(url, {
            method: 'POST',
            headers: {
                'User-Agent': UA,
                'Referer': BASE + '/',
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: body
        });
        if (!res.ok) return [];
        var html = await res.text();
        var $ = cheerio.load(html);
        var results = [];
        $('a.short-poster').each(function () {
            var href = $(this).attr('href') || '';
            var alt = $(this).attr('alt') || '';
            var m = href.match(/newsid=(\d+)/);
            if (m) {
                results.push({ newsid: m[1], title: alt.trim() });
            }
        });
        return results;
    } catch (e) {
        return [];
    }
}

function findBestResult(results, targetTitle, targetYear, season) {
    if (!results.length) return null;
    var normTarget = normalize(targetTitle);

    if (season) {
        var seasonStr = 'saison' + season;
        for (var i = 0; i < results.length; i++) {
            var r = results[i];
            var normFound = normalize(r.title);
            if (normFound.indexOf(normTarget) !== -1 && normFound.indexOf(seasonStr) !== -1) return r;
        }
    }

    for (var i = 0; i < results.length; i++) {
        var r = results[i];
        var normFound = normalize(r.title);
        if (normFound === normTarget) return r;
    }
    for (var i = 0; i < results.length; i++) {
        var r = results[i];
        var normFound = normalize(r.title);
        if (normFound.indexOf(normTarget) !== -1 || normTarget.indexOf(normFound) !== -1) return r;
    }
    return results[0];
}

function buildEmbedHeaders(url) {
    var h = { 'Referer': BASE + '/' };
    try {
        var u = new URL(url);
        h['Referer'] = u.origin + '/';
    } catch (e) {}
    return h;
}

async function getFilmStreams(newsid) {
    var url = BASE + '/engine/ajax/film_api.php?id=' + newsid;
    try {
        var res = await fetch(url, { headers: HEADERS });
        if (!res.ok) return [];
        var data = await res.json();
        var players = data.players || {};
        var streams = [];
        var playerNames = ['vidzy', 'uqload', 'dood', 'voe', 'premium'];
        var versionKeys = ['default', 'vostfr', 'vfq', 'vff'];
        var versionLabels = { default: '', vostfr: ' VOSTFR', vfq: ' VF', vff: ' VF' };

        for (var pi = 0; pi < playerNames.length; pi++) {
            var pname = playerNames[pi];
            var pdata = players[pname];
            if (!pdata) continue;
            for (var vi = 0; vi < versionKeys.length; vi++) {
                var vk = versionKeys[vi];
                var streamUrl = pdata[vk] || pdata['default'] || '';
                if (!streamUrl) continue;
                var quality = 'HD';
                if (pdata.quality) quality = pdata.quality;
                var label = pname.charAt(0).toUpperCase() + pname.slice(1);
                var versionLabel = versionLabels[vk] || '';
                streams.push({
                    url: streamUrl,
                    title: label + versionLabel,
                    name: 'French Stream',
                    quality: quality,
                    language: vk === 'vostfr' ? 'VOSTFR' : 'VF',
                    provider: 'fs16.lol',
                    type: 'mp4',
                    headers: buildEmbedHeaders(streamUrl)
                });
            }
        }
        return streams;
    } catch (e) {
        return [];
    }
}

async function getSeriesStreams(newsid, season, episode) {
    var url = BASE + '/static/series/' + newsid + '.js';
    try {
        var res = await fetch(url, { headers: HEADERS });
        if (!res.ok) return [];
        var data = await res.json();
        var streams = [];
        var versions = ['vf', 'vostfr', 'vo'];
        var playerNames = ['vidzy', 'uqload', 'premium', 'voe', 'netu'];
        var versionLabels = { vf: ' VF', vostfr: ' VOSTFR', vo: ' VO' };

        for (var vi = 0; vi < versions.length; vi++) {
            var ver = versions[vi];
            var episodes = data[ver];
            if (!episodes || !episodes[episode]) continue;
            var epData = episodes[episode];
            for (var pi = 0; pi < playerNames.length; pi++) {
                var pname = playerNames[pi];
                var streamUrl = epData[pname];
                if (!streamUrl) continue;
                var label = pname.charAt(0).toUpperCase() + pname.slice(1);
                var versionLabel = versionLabels[ver] || '';
                streams.push({
                    url: streamUrl,
                    title: 'S' + season + 'E' + episode + ' - ' + label + versionLabel,
                    name: 'French Stream',
                    quality: 'HD',
                    language: ver === 'vostfr' ? 'VOSTFR' : ver === 'vo' ? 'VO' : 'VF',
                    provider: 'fs16.lol',
                    type: 'mp4',
                    headers: buildEmbedHeaders(streamUrl)
                });
            }
        }
        return streams;
    } catch (e) {
        return [];
    }
}

async function findFs16Entry(title, year, mediaType, season) {
    var searchQuery = title;
    var results = await searchFs16(searchQuery);
    var best = findBestResult(results, title, year, season);
    return best;
}

module.exports.getStreams = async function (tmdbId, mediaType, season, episode) {
    try {
        var metadata = await tmdbLookup(tmdbId, mediaType);
        if (!metadata) return [];

        var title = metadata.title || metadata.name || '';
        var year = '';
        if (metadata.release_date) year = metadata.release_date.substring(0, 4);
        else if (metadata.first_air_date) year = metadata.first_air_date.substring(0, 4);

        var entry = await findFs16Entry(title, year, mediaType, season);
        if (!entry) return [];

        if (mediaType === 'tv' && season && episode) {
            return await getSeriesStreams(entry.newsid, season, episode);
        } else {
            return await getFilmStreams(entry.newsid);
        }
    } catch (e) {
        console.log('[fr-stream] Error: ' + e.message);
        return [];
    }
};
