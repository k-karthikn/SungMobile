package com.karthi.sungmobile.core.youtube.home

import com.karthi.sungmobile.core.model.CatalogKind
import org.junit.Assert.*
import org.junit.Test

class MusicBrowseParserTest {
    @Test fun `home maps playable songs and openable collections, skipping unknown items`() {
        val first = MusicBrowseParser.home("""{
          "responseContext":{"visitorData":"visitor-test"},
          "contents":{"singleColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{"content":{
            "sectionListRenderer":{"contents":[
              {"musicCarouselShelfRenderer":{"header":{"musicCarouselShelfBasicHeaderRenderer":{"title":{"runs":[{"text":"Quick picks"}]}}},
                "contents":[${songRow("AAAAAAAAAAA")},{"musicMultiRowListItemRenderer":{}}]}},
              {"musicCarouselShelfRenderer":{"header":{"musicCarouselShelfBasicHeaderRenderer":{"title":{"runs":[{"text":"Collections"}]}}},
                "contents":[${collectionRow("MUSIC_PAGE_TYPE_PLAYLIST", "VLPLabcdefghijklmnopqrst")},
                            ${collectionRow("MUSIC_PAGE_TYPE_ALBUM", "MPREb_abcdefghijkl")}]}}
            ],"continuations":[{"nextContinuationData":{"continuation":"next-token"}}]}
          }}}]}}
        }""")
        assertEquals(listOf("Quick picks", "Collections"), first.shelves.map { it.title })
        assertEquals("visitor-test", first.visitorData)
        assertEquals("next-token", first.continuation)
        assertEquals(listOf(CatalogKind.PLAYLIST, CatalogKind.ALBUM), first.shelves[1].items.map { it.kind })
        assertEquals("https://music.youtube.com/browse/MPREb_abcdefghijkl", first.shelves[1].items[1].url)
        assertEquals("https://www.youtube.com/watch?v=AAAAAAAAAAA", first.shelves[0].items.single().url)
        assertEquals("https://yt3.googleusercontent.com/art=w544-h544-l90-rj", first.shelves[0].items.single().artworkUrl)
    }

    @Test fun `song artist runs keep provider punctuation but omit play counts`() {
        val home = MusicBrowseParser.home("""{
          "contents":{"singleColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{"content":{
            "sectionListRenderer":{"contents":[{"musicCarouselShelfRenderer":{
              "header":{"musicCarouselShelfBasicHeaderRenderer":{"title":{"runs":[{"text":"Quick picks"}]}}},
              "contents":[{"musicTwoRowItemRenderer":{
                "title":{"runs":[{"text":"KALYANI","navigationEndpoint":{"watchEndpoint":{"videoId":"AAAAAAAAAAA"}}}]},
                "subtitle":{"runs":[{"text":"ARJN"},{"text":", "},{"text":"KDS"},{"text":", "},{"text":"FIFTY4"},{"text":" • "},{"text":"622M plays"}]}
              }}, {"musicResponsiveListItemRenderer":{"flexColumns":[
                {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Boom Shaka","navigationEndpoint":{"watchEndpoint":{"videoId":"BBBBBBBBBBB"}}}]}}},
                {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"KR${'$'}NA"},{"text":" & "},{"text":"Dhanda Nyoliwala"},{"text":" • "},{"text":"203M plays"}]}}}
              ]}}]}}]}
          }}}]}}
        }""")
        assertEquals(listOf("ARJN, KDS, FIFTY4", "KR${'$'}NA & Dhanda Nyoliwala"),
            home.shelves.single().items.map { it.subtitle })
    }

    @Test fun `continuation maps rows and malformed or unsupported entries do not become songs`() {
        val next = MusicBrowseParser.home("""{
          "continuationContents":{"sectionListContinuation":{"contents":[
            {"musicCarouselShelfRenderer":{"header":{"musicCarouselShelfBasicHeaderRenderer":{"strapline":{"runs":[{"text":"New music"}]}}},
              "contents":[${collectionRow("MUSIC_PAGE_TYPE_ALBUM", "MPREb_abcdefghijkl")},
                          ${collectionRow("MUSIC_PAGE_TYPE_AUDIOBOOK", "MPREb_abcdefghijkl")}]
            }}
          ]}}
        }""", 5)
        assertEquals("New music", next.shelves.single().title)
        assertEquals(1, next.shelves.single().items.size)
        assertEquals(CatalogKind.ALBUM, next.shelves.single().items.single().kind)
        assertTrue(MusicBrowseParser.home("{}") .shelves.isEmpty())
    }

    @Test fun `album browse and playlist browse expose actual track rows`() {
        val browse = """{
          "contents":{"twoColumnBrowseResultsRenderer":{
            "tabs":[{"tabRenderer":{"content":{"sectionListRenderer":{"contents":[
              {"musicResponsiveHeaderRenderer":{"title":{"runs":[{"text":"Collection name"}]},
                "straplineTextOne":{"runs":[{"text":"Creator"}]},
                "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"https://example.com/cover.jpg"}]}}}}}
            ]}}}}],
            "secondaryContents":{"sectionListRenderer":{"contents":[
              {"musicShelfRenderer":{"contents":[${songRow("AAAAAAAAAAA")},${songRow("BBBBBBBBBBB")}]}}
            ]}}
          }}
        }"""
        val album = MusicBrowseParser.collection(browse, "https://music.youtube.com/browse/MPREb_abcdefghijkl", CatalogKind.ALBUM)
        assertEquals("Collection name", album.title)
        assertEquals(2, album.items.size)
        assertEquals("Creator", album.details?.creator)
        assertEquals("https://yt3.googleusercontent.com/art=w544-h544-l90-rj", album.items.first().artworkUrl)
        val playlist = MusicBrowseParser.collection(browse.replace("musicShelfRenderer", "musicPlaylistShelfRenderer"),
            "https://music.youtube.com/browse/VLPLabcdefghijklmnopqrst", CatalogKind.PLAYLIST)
        assertEquals(2, playlist.items.size)
    }

    private fun songRow(id: String) = """{"musicResponsiveListItemRenderer":{
      "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"https://yt3.googleusercontent.com/art=w120-h120-l90-rj"}]}}},
      "flexColumns":[
        {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Track title","navigationEndpoint":{"watchEndpoint":{"videoId":"$id"}}}]}}},
        {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Artist"}]}}}
      ]}}"""

    private fun collectionRow(type: String, id: String) = """{"musicTwoRowItemRenderer":{
      "title":{"runs":[{"text":"Collection title","navigationEndpoint":{"browseEndpoint":{
        "browseId":"$id","browseEndpointContextSupportedConfigs":{"browseEndpointContextMusicConfig":{"pageType":"$type"}}
      }}}]},
      "subtitle":{"runs":[{"text":"Curator"}]}
    }}"""
}
