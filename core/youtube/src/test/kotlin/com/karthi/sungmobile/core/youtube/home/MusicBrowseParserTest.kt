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

    @Test fun `artist shelf rows remain openable in Home`() {
        val home = MusicBrowseParser.home("""{
          "contents":{"singleColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{"content":{
            "sectionListRenderer":{"contents":[{"musicCarouselShelfRenderer":{
              "header":{"musicCarouselShelfBasicHeaderRenderer":{"title":{"runs":[{"text":"Artists for you"}]}}},
              "contents":[${artistRow("UC1234567890123456789012")}]}}
            ]}
          }}}]}}
        }""")
        val artist = home.shelves.single().items.single()
        assertEquals(CatalogKind.ARTIST, artist.kind)
        assertEquals("https://music.youtube.com/browse/UC1234567890123456789012", artist.url)
    }

    @Test fun `immersive artist pages expose song shelf rows and artist artwork`() {
        val page = MusicBrowseParser.collection("""{
          "header":{"musicImmersiveHeaderRenderer":{
            "title":{"runs":[{"text":"John Michael Howell - Topic"}]},
            "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[
              {"url":"https://yt3.googleusercontent.com/artist=w300-h300-l90-rj"}
            ]}}}
          }},
          "contents":{"singleColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{"content":{
            "sectionListRenderer":{"contents":[{"musicShelfRenderer":{"title":{"runs":[{"text":"Songs"}]},
              "contents":[${songRow("AAAAAAAAAAA")},${songRow("BBBBBBBBBBB")}]}}
            ]}
          }}}]}}
        }""", "https://music.youtube.com/browse/UC1234567890123456789012", CatalogKind.ARTIST)
        assertEquals("John Michael Howell - Topic", page.title)
        assertEquals(2, page.items.size)
        assertEquals(CatalogKind.ARTIST, page.details?.kind)
        assertEquals("https://yt3.googleusercontent.com/artist=w544-h544-l90-rj", page.details?.artworkUrl)
    }

    @Test fun `podcast Home shelves map shows and episodes to openable rows`() {
        val shelves = MusicBrowseParser.podcastShelves("""{
          "contents":{"carousel":{"musicCarouselShelfRenderer":{
            "header":{"musicCarouselShelfBasicHeaderRenderer":{"title":{"runs":[{"text":"Podcasts for you"}]}}},
            "contents":[
              ${podcastShowRow("MPSPPLabcde12345")},
              {"musicMultiRowListItemRenderer":{"title":{"runs":[{"text":"Episode one"}]},
                "onTap":{"watchEndpoint":{"videoId":"EEEEEEEEEEE"}},
                "subtitle":{"runs":[{"text":"The daily show"}]}}}
            ]
          }}}
        }""")
        val shelf = shelves.single()
        assertEquals("Podcasts for you", shelf.title)
        assertEquals(listOf(CatalogKind.PLAYLIST, CatalogKind.SONG), shelf.items.map { it.kind })
        assertEquals("https://music.youtube.com/browse/MPSPPLabcde12345", shelf.items.first().url)
        assertEquals("https://www.youtube.com/watch?v=EEEEEEEEEEE", shelf.items.last().url)
    }

    @Test fun `podcast show cards stay out of the general music Home feed`() {
        val home = MusicBrowseParser.home("""{
          "contents":{"singleColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{"content":{
            "sectionListRenderer":{"contents":[{"musicCarouselShelfRenderer":{
              "header":{"musicCarouselShelfBasicHeaderRenderer":{"title":{"runs":[{"text":"Popular shows"}]}}},
              "contents":[${podcastShowRow("MPSPPLabcde12345")}]}}
            ]}
          }}}]}}
        }""")
        assertTrue(home.shelves.isEmpty())
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

    private fun artistRow(id: String) = """{"musicTwoRowItemRenderer":{
      "title":{"runs":[{"text":"Artist name","navigationEndpoint":{"browseEndpoint":{
        "browseId":"$id","browseEndpointContextSupportedConfigs":{"browseEndpointContextMusicConfig":{"pageType":"MUSIC_PAGE_TYPE_ARTIST"}}
      }}}]},"subtitle":{"runs":[{"text":"Artist"}]}
    }}"""

    private fun podcastShowRow(id: String) = """{"musicTwoRowItemRenderer":{
      "title":{"runs":[{"text":"Podcast show"}]},"subtitle":{"runs":[{"text":"Publisher"}]},
      "navigationEndpoint":{"browseEndpoint":{"browseId":"$id"}}
    }}"""
}
