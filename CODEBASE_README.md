# LibreTube Fork Codebase Notes

This note explains how this fork uses Piped and NewPipeExtractor, and where to look when YouTube extraction breaks. It is meant to be handed to another AI or maintainer before asking them to compare this codebase with a newer NewPipeExtractor.

## What This Fork Does

LibreTube has one app-facing media repository interface:

- `app/src/main/java/com/github/libretube/api/MediaServiceRepository.kt`

That interface hides two possible backends:

- Piped API: uses Retrofit and a Piped instance.
- Local extraction: uses NewPipeExtractor directly against YouTube, then converts the extractor output into the same app models used by the Piped path.

The important design rule is: UI, player, downloads, and feeds should keep consuming LibreTube's own models (`Streams`, `PipedStream`, `StreamItem`, `Channel`, `Playlist`, `CommentsPage`) without caring whether the data came from Piped or NewPipeExtractor.

## Dependency

The pinned extractor version is in:

- `gradle/libs.versions.toml`

Look for:

```toml
newpipeextractor = "..."
newpipeextractor = { module = "com.github.libre-tube:NewPipeExtractor", version.ref = "newpipeextractor" }
```

The app includes it in:

- `app/build.gradle.kts`

```kotlin
implementation(libs.newpipeextractor)
```

When playback/search/channel extraction breaks after YouTube changes, compare this pinned extractor commit with the latest working NewPipeExtractor commit.

## Backend Selection

`MediaServiceRepository.instance` chooses the backend:

```kotlin
PlayerHelper.fullLocalMode -> NewPipeMediaServiceRepository()
PlayerHelper.localStreamExtraction -> LocalStreamsExtractionPipedMediaServiceRepository()
else -> PipedMediaServiceRepository()
```

Related files:

- `app/src/main/java/com/github/libretube/api/MediaServiceRepository.kt`
- `app/src/main/java/com/github/libretube/helpers/PlayerHelper.kt`
- `app/src/main/java/com/github/libretube/constants/PreferenceKeys.kt`

Meaning:

- `fullLocalMode`: use NewPipeExtractor for the main media operations.
- `localStreamExtraction`: use Piped for most metadata, but use NewPipeExtractor for stream URLs through `getStreams(videoId)`.
- neither enabled: use the Piped API path.

## Main Extractor Adapter

Most extractor compatibility work belongs in:

- `app/src/main/java/com/github/libretube/api/NewPipeMediaServiceRepository.kt`

This file calls NewPipeExtractor and maps extractor classes into LibreTube/Piped-shaped app models.

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

If a NewPipeExtractor update causes compile errors, start in this file.

## Extractor Initialization

Files:

- `app/src/main/java/com/github/libretube/helpers/NewPipeExtractorInstance.kt`
- `app/src/main/java/com/github/libretube/util/NewPipeDownloaderImpl.kt`
- `app/src/main/java/com/github/libretube/LibreTubeApp.kt`

`LibreTubeApp` calls `NewPipeExtractorInstance.init()`.

`NewPipeExtractorInstance` calls:

```kotlin
NewPipe.init(NewPipeDownloaderImpl())
NewPipe.getService(ServiceList.YouTube.serviceId)
```

If NewPipeExtractor changes downloader, request, response, service, localization, or initialization APIs, these files may need changes.

## Piped API Path

Files:

- `app/src/main/java/com/github/libretube/api/PipedMediaServiceRepository.kt`
- `app/src/main/java/com/github/libretube/api/PipedApi.kt`
- `app/src/main/java/com/github/libretube/api/RetrofitInstance.kt`
- `app/src/main/java/com/github/libretube/api/obj/*.kt`

The local extractor path should keep matching the same model shape returned by the Piped API path. This keeps the rest of the app stable.

The hybrid local-stream mode is:

- `app/src/main/java/com/github/libretube/api/LocalStreamsExtractionPipedMediaServiceRepository.kt`

It extends `PipedMediaServiceRepository` and overrides only:

```kotlin
override suspend fun getStreams(videoId: String) = newPipeDelegate.getStreams(videoId)
```

If only playback URLs fail but Piped metadata still loads, inspect this path and `NewPipeMediaServiceRepository.getStreams(...)`.

## Stream Model Fields To Protect

Playback, downloads, quality selection, DASH, subtitles, and audio track selection depend on:

- `app/src/main/java/com/github/libretube/api/obj/Streams.kt`
- `app/src/main/java/com/github/libretube/api/obj/PipedStream.kt`
- `app/src/main/java/com/github/libretube/helpers/DashHelper.kt`
- `app/src/main/java/com/github/libretube/helpers/PlayerHelper.kt`
- `app/src/main/java/com/github/libretube/ui/dialogs/DownloadDialog.kt`

When updating extractor mappings, check these `PipedStream` fields carefully:

- `url`
- `format`
- `quality`
- `mimeType`
- `codec`
- `videoOnly`
- `bitrate`
- `initStart`
- `initEnd`
- `indexStart`
- `indexEnd`
- `width`
- `height`
- `fps`
- `audioTrackId`
- `audioTrackName`
- `audioTrackLocale`
- `audioTrackType`
- `contentLength`

## PO Token Integration

Files:

- `app/src/main/java/com/github/libretube/api/poToken/PoTokenGenerator.kt`
- `app/src/main/java/com/github/libretube/api/poToken/PoTokenWebView.kt`
- `app/src/main/java/com/github/libretube/api/poToken/JavaScriptUtil.kt`
- `app/src/main/java/com/github/libretube/api/NewPipeMediaServiceRepository.kt`

`NewPipeMediaServiceRepository` currently registers:

```kotlin
YoutubeStreamExtractor.setPoTokenProvider(PoTokenGenerator())
```

If playback fails with signature, bot-check, visitor-data, PO-token, or YouTube client errors, compare this implementation with the current NewPipeExtractor YouTube PO-token API:

- `PoTokenProvider`
- `PoTokenResult`
- `YoutubeStreamExtractor.setPoTokenProvider(...)`
- `YoutubeParsingHelper.getVisitorDataFromInnertube(...)`
- `InnertubeClientRequestInfo.WEB`

## Common Break Points

Compile errors after extractor update usually mean:

- extractor property/method renamed
- return type changed
- PO-token interface changed
- `Downloader.execute(...)`, `Request`, or `Response` changed
- `Page` or `ListLinkHandler` changed

Playback opens but video does not play:

- check `NewPipeMediaServiceRepository.getStreams(...)`
- check `VideoStream.toPipedStream()`
- check `AudioStream.toPipedStream()`
- check `DashHelper.kt`
- check PO-token files

Search, channels, playlists, or comments fail:

- check `InfoItem` subclasses
- check pagination with `Page`
- check channel tabs with `ListLinkHandler`
- check `ContentAvailability`
- check thumbnail/avatar list APIs
- check date APIs such as `uploadDate.offsetDateTime()`

Trending fails:

- check `NewPipeMediaServiceRepository.trendingCategories`
- compare local kiosk IDs with current NewPipeExtractor YouTube kiosk IDs

Current kiosk IDs used by this fork:

- `trending_gaming`
- `trending_movies_and_shows`
- `trending_podcasts_episodes`
- `trending_music`
- `live`

## AI Handoff Prompt

Use this prompt when asking another AI to fix extractor breakage:

```text
You are working in this LibreTube fork. Read CODEBASE_README.md first.

The app pins NewPipeExtractor in gradle/libs.versions.toml. It maps NewPipeExtractor output into LibreTube's Piped-shaped models in app/src/main/java/com/github/libretube/api/NewPipeMediaServiceRepository.kt.

Compare this codebase with the latest or specified NewPipeExtractor source. Find API or behavior changes affecting:
- NewPipe initialization/downloader
- StreamInfo, VideoStream, AudioStream, and stream URL extraction
- PO-token provider integration
- search, channel, playlist, and comment extraction
- Page and ListLinkHandler pagination

Tell me exactly which local files and mappings need to change. If possible, implement the smallest compatible fix and run ./gradlew.bat :app:assembleDebug.
```

## Verification

After extractor-related changes, run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Then test:

- normal video playback
- livestream playback
- Shorts or vertical video playback
- quality switching
- audio track/language switching
- video/audio download
- search
- channel tabs
- playlist next page
- comments next page
- full local mode
- local stream extraction mode

Keep unrelated UI, database, translation, and dependency changes out of extractor fixes unless the failure proves they are involved.
