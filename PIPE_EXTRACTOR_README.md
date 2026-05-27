# Pipe / NewPipe Extractor Maintenance Notes

This file is for future debugging when YouTube, Piped, or NewPipeExtractor changes and this LibreTube build starts failing. It is written as a handoff note for an AI or maintainer who needs to compare this app against a newer NewPipeExtractor and identify what must be changed locally.

## Quick Summary

LibreTube has two data paths:

- Piped API path: talks to a configured Piped instance through Retrofit.
- Local extractor path: talks directly to YouTube using NewPipeExtractor and maps extractor models into the same app models used by the Piped path.

The local extractor path is the important part when YouTube breaks playback, search, channel pages, playlists, comments, or stream extraction.

## Current Extractor Dependency

The NewPipeExtractor dependency is declared in:

- `gradle/libs.versions.toml`

Current value:

```toml
newpipeextractor = "c8b50dff8"
newpipeextractor = { module = "com.github.libre-tube:NewPipeExtractor", version.ref = "newpipeextractor" }
```

The app dependency is wired in:

- `app/build.gradle.kts`

```kotlin
implementation(libs.newpipeextractor)
```

When updating the extractor, compare this pinned commit/version with the newer extractor API and migration notes.

## Runtime Mode Selection

The repository abstraction is:

- `app/src/main/java/com/github/libretube/api/MediaServiceRepository.kt`

Selection logic:

```kotlin
PlayerHelper.fullLocalMode -> NewPipeMediaServiceRepository()
PlayerHelper.localStreamExtraction -> LocalStreamsExtractionPipedMediaServiceRepository()
else -> PipedMediaServiceRepository()
```

Preferences are in:

- `app/src/main/java/com/github/libretube/helpers/PlayerHelper.kt`
- `app/src/main/java/com/github/libretube/constants/PreferenceKeys.kt`

Current defaults in `PlayerHelper`:

- `fullLocalMode` defaults to `true`
- `localStreamExtraction` defaults to `true`
- `localRYD` defaults to `true`

Meaning:

- Full local mode uses NewPipeExtractor for most media operations.
- Local stream extraction mode uses Piped for metadata but NewPipeExtractor only for `getStreams(videoId)`.
- If both local modes are disabled, Piped handles everything.

## Main Files To Check

### Extractor Initialization

- `app/src/main/java/com/github/libretube/helpers/NewPipeExtractorInstance.kt`
- `app/src/main/java/com/github/libretube/util/NewPipeDownloaderImpl.kt`
- `app/src/main/java/com/github/libretube/LibreTubeApp.kt`

`LibreTubeApp` calls `NewPipeExtractorInstance.init()`.

`NewPipeExtractorInstance` initializes NewPipe with `NewPipeDownloaderImpl`, then lazily exposes the YouTube `StreamingService`.

If NewPipeExtractor changes its initialization, downloader, localization, content-country, or service API, start here.

### Local Extractor Adapter

- `app/src/main/java/com/github/libretube/api/NewPipeMediaServiceRepository.kt`

This is the main compatibility layer. It converts NewPipeExtractor objects into LibreTube/Piped-shaped objects.

Important mapping functions:

- `VideoStream.toPipedStream()`
- `AudioStream.toPipedStream()`
- `StreamInfoItem.toStreamItem()`
- `InfoItem.toContentItem()`
- `ChannelInfo.toChannel()`
- `PlaylistInfo.toPlaylist()`
- `CommentsInfoItem.toComment()`
- `Page.toNextPageString()`
- `String.toPage()`
- `ListLinkHandler.toTabDataString()`
- `String.toListLinkHandler()`

Important extractor calls:

- `StreamInfo.getInfo(...)`
- `SearchInfo.getInfo(...)`
- `SearchInfo.getMoreItems(...)`
- `ChannelInfo.getInfo(...)`
- `ChannelTabInfo.getInfo(...)`
- `ChannelTabInfo.getMoreItems(...)`
- `PlaylistInfo.getInfo(...)`
- `PlaylistInfo.getMoreItems(...)`
- `CommentsInfo.getInfo(...)`
- `CommentsInfo.getMoreItems(...)`
- `KioskInfo.getInfo(...)`
- `NewPipeExtractorInstance.extractor.searchQHFactory.fromQuery(...)`
- `NewPipeExtractorInstance.extractor.suggestionExtractor.suggestionList(...)`
- `NewPipeExtractorInstance.extractor.kioskList`

If the app fails to compile after a NewPipeExtractor update, most fixes are likely in this file.

### Piped Path

- `app/src/main/java/com/github/libretube/api/PipedMediaServiceRepository.kt`
- `app/src/main/java/com/github/libretube/api/PipedApi.kt`
- `app/src/main/java/com/github/libretube/api/RetrofitInstance.kt`
- `app/src/main/java/com/github/libretube/api/obj/*.kt`

The Piped path expects response models such as `Streams`, `PipedStream`, `StreamItem`, `Channel`, `Playlist`, and `CommentsPage`.

The local extractor adapter must keep returning these same app models so the UI/player/download code does not care whether data came from Piped or NewPipeExtractor.

### Hybrid Stream Extraction

- `app/src/main/java/com/github/libretube/api/LocalStreamsExtractionPipedMediaServiceRepository.kt`

This class extends `PipedMediaServiceRepository` but delegates only `getStreams(videoId)` to `NewPipeMediaServiceRepository`.

If only playback URLs break but Piped metadata still works, check this mode first.

### Player / Download Model Expectations

- `app/src/main/java/com/github/libretube/api/obj/Streams.kt`
- `app/src/main/java/com/github/libretube/api/obj/PipedStream.kt`
- `app/src/main/java/com/github/libretube/helpers/DashHelper.kt`
- `app/src/main/java/com/github/libretube/helpers/PlayerHelper.kt`
- `app/src/main/java/com/github/libretube/ui/dialogs/DownloadDialog.kt`

The extractor adapter must populate enough fields for playback, DASH manifest creation, quality selection, subtitles, downloads, and audio tracks.

Fields especially likely to matter:

- `PipedStream.url`
- `PipedStream.format`
- `PipedStream.quality`
- `PipedStream.mimeType`
- `PipedStream.codec`
- `PipedStream.videoOnly`
- `PipedStream.bitrate`
- `PipedStream.initStart`
- `PipedStream.initEnd`
- `PipedStream.indexStart`
- `PipedStream.indexEnd`
- `PipedStream.width`
- `PipedStream.height`
- `PipedStream.fps`
- `PipedStream.audioTrackId`
- `PipedStream.audioTrackName`
- `PipedStream.audioTrackLocale`
- `PipedStream.audioTrackType`
- `PipedStream.contentLength`

## PO Token Integration

Files:

- `app/src/main/java/com/github/libretube/api/poToken/PoTokenGenerator.kt`
- `app/src/main/java/com/github/libretube/api/poToken/PoTokenWebView.kt`
- `app/src/main/java/com/github/libretube/api/poToken/JavaScriptUtil.kt`
- `app/src/main/java/com/github/libretube/api/NewPipeMediaServiceRepository.kt`

`NewPipeMediaServiceRepository` currently runs:

```kotlin
YoutubeStreamExtractor.setPoTokenProvider(PoTokenGenerator())
```

If playback fails with signature, bot-check, visitor-data, PO-token, or client errors, compare this code with the current NewPipeExtractor YouTube PO-token API:

- `PoTokenProvider`
- `PoTokenResult`
- `YoutubeStreamExtractor.setPoTokenProvider(...)`
- `YoutubeParsingHelper.getVisitorDataFromInnertube(...)`
- `InnertubeClientRequestInfo.WEB`

NewPipeExtractor may rename or change these interfaces.

## What To Compare Against NewPipeExtractor

When upgrading or debugging, inspect upstream NewPipeExtractor for these packages/classes:

- `org.schabi.newpipe.extractor.NewPipe`
- `org.schabi.newpipe.extractor.ServiceList`
- `org.schabi.newpipe.extractor.StreamingService`
- `org.schabi.newpipe.extractor.downloader.Downloader`
- `org.schabi.newpipe.extractor.downloader.Request`
- `org.schabi.newpipe.extractor.downloader.Response`
- `org.schabi.newpipe.extractor.stream.StreamInfo`
- `org.schabi.newpipe.extractor.stream.VideoStream`
- `org.schabi.newpipe.extractor.stream.AudioStream`
- `org.schabi.newpipe.extractor.stream.StreamInfoItem`
- `org.schabi.newpipe.extractor.stream.ContentAvailability`
- `org.schabi.newpipe.extractor.search.SearchInfo`
- `org.schabi.newpipe.extractor.channel.ChannelInfo`
- `org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo`
- `org.schabi.newpipe.extractor.channel.tabs.ChannelTabs`
- `org.schabi.newpipe.extractor.comments.CommentsInfo`
- `org.schabi.newpipe.extractor.comments.CommentsInfoItem`
- `org.schabi.newpipe.extractor.playlist.PlaylistInfo`
- `org.schabi.newpipe.extractor.kiosk.KioskInfo`
- `org.schabi.newpipe.extractor.Page`
- `org.schabi.newpipe.extractor.linkhandler.ListLinkHandler`
- `org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor`
- `org.schabi.newpipe.extractor.services.youtube.PoTokenProvider`
- `org.schabi.newpipe.extractor.services.youtube.PoTokenResult`

## Common Breakage Patterns

### Compile Errors After Extractor Update

Likely causes:

- A NewPipeExtractor property or method was renamed.
- A return type changed.
- PO-token interfaces changed.
- `Downloader.execute(...)`, `Request`, or `Response` constructor changed.
- `Page` or `ListLinkHandler` fields changed.

Fix approach:

1. Find the failing symbol in `NewPipeMediaServiceRepository.kt`, `NewPipeDownloaderImpl.kt`, or `PoTokenGenerator.kt`.
2. Open the same class in the newer NewPipeExtractor.
3. Update the adapter mapping while preserving LibreTube's app model output.

### Playback Opens But Video Does Not Play

Likely files:

- `NewPipeMediaServiceRepository.getStreams(...)`
- `VideoStream.toPipedStream()`
- `AudioStream.toPipedStream()`
- `PipedStream.kt`
- `DashHelper.kt`
- PO-token files

Check whether extractor stream fields still map correctly:

- direct URL field, currently `content`
- format/mime type
- codec
- itag content length
- DASH init/index ranges
- audio language/track metadata
- video-only vs muxed streams

### Search, Channels, Playlists, Or Comments Break

Likely files:

- `NewPipeMediaServiceRepository.kt`
- `CategoryFeedRepository.kt`
- `LocalFeedRepository.kt`
- `LocalSubscriptionsRepository.kt`

Check:

- `InfoItem` subclasses
- pagination via `Page`
- channel tab serialization via `ListLinkHandler`
- availability filtering via `ContentAvailability`
- thumbnail/avatar list APIs
- date APIs such as `uploadDate.offsetDateTime()`

### Pagination Breaks

This app serializes extractor pagination objects into strings because the rest of LibreTube expects Piped-style `nextpage` strings.

Check:

- `Page.toNextPageString()`
- `String.toPage()`
- `ListLinkHandler.toTabDataString()`
- `String.toListLinkHandler()`

If NewPipeExtractor changes `Page` or `ListLinkHandler`, update these serializers and all callers.

### Trending Categories Break

Check `NewPipeMediaServiceRepository.trendingCategories`.

Current kiosk IDs:

- `trending_gaming`
- `trending_movies_and_shows`
- `trending_podcasts_episodes`
- `trending_music`
- `live`

Compare with the current YouTube kiosk IDs in NewPipeExtractor.

## Suggested AI Prompt For Future Fixes

Use this prompt when asking another AI to diagnose a break:

```text
You are working in this LibreTube fork. Read PIPE_EXTRACTOR_README.md first.

The app uses a pinned NewPipeExtractor dependency from gradle/libs.versions.toml and maps NewPipeExtractor objects into LibreTube's Piped-shaped models in app/src/main/java/com/github/libretube/api/NewPipeMediaServiceRepository.kt.

Please compare the current code against the latest or specified NewPipeExtractor source. Find API changes or behavior changes that affect:
- NewPipe initialization/downloader
- StreamInfo, VideoStream, AudioStream, and stream URL extraction
- PO-token provider integration
- Search/channel/playlist/comment extraction
- Page and ListLinkHandler pagination

Then tell me exactly which local files and mappings need to change. If possible, implement the smallest compatible fix and run a build.
```

## Suggested Verification

After changing extractor-related code, run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Then manually test these flows:

- Open a normal video.
- Open a livestream.
- Open a Short or vertical video.
- Change video quality.
- Change audio track or language if available.
- Download video/audio.
- Search videos.
- Open a channel and switch tabs.
- Open a playlist and scroll to next page.
- Load comments and comment next page.
- Try both full local mode and local stream extraction mode.

## Do Not Accidentally Change

Avoid changing UI, database, translations, or unrelated Gradle dependencies while fixing extractor compatibility unless the compiler or runtime failure proves they are involved.

The useful boundary is:

- Keep `MediaServiceRepository` output models stable.
- Adapt NewPipeExtractor changes inside the local extractor adapter.
- Let existing player/UI/download code continue consuming `Streams`, `PipedStream`, `StreamItem`, `Channel`, `Playlist`, and `CommentsPage`.
