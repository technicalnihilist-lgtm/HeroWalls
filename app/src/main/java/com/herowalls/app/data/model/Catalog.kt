package com.herowalls.app.data.model

enum class CollectionGroup(val title: String, val tagline: String) {
    MARVEL("Marvel Universe", "Earth's mightiest artwork"),
    DC("DC Universe", "Guardians of the DC multiverse"),
    VILLAINS("Villains", "The darker side of the panel"),
    COLLECTIONS("Collections", "Curated superhero collections")
}

/**
 * A browsable category. [searchTerm] is the exact phrase sent to the Wikimedia
 * Commons search API - categories are queries, never hard-coded image lists, so
 * new uploads appear automatically.
 */
data class HeroCategory(
    val id: String,
    val name: String,
    val searchTerm: String,
    val group: CollectionGroup,
    val blurb: String = ""
)

object Catalog {

    val marvel = listOf(
        HeroCategory("spider-man", "Spider-Man", "Spider-Man", CollectionGroup.MARVEL, "Your friendly neighbourhood wall-crawler"),
        HeroCategory("iron-man", "Iron Man", "Iron Man", CollectionGroup.MARVEL, "Genius, billionaire, playboy, philanthropist"),
        HeroCategory("captain-america", "Captain America", "Captain America", CollectionGroup.MARVEL, "The first Avenger"),
        HeroCategory("thor", "Thor", "Thor (Marvel Comics)", CollectionGroup.MARVEL, "God of Thunder"),
        HeroCategory("hulk", "Hulk", "Hulk", CollectionGroup.MARVEL, "The strongest there is"),
        HeroCategory("black-panther", "Black Panther", "Black Panther (character)", CollectionGroup.MARVEL, "Wakanda forever"),
        HeroCategory("doctor-strange", "Doctor Strange", "Doctor Strange", CollectionGroup.MARVEL, "Master of the mystic arts"),
        HeroCategory("black-widow", "Black Widow", "Black Widow (Natasha Romanova)", CollectionGroup.MARVEL, "Red room graduate"),
        HeroCategory("captain-marvel", "Captain Marvel", "Captain Marvel (Marvel Comics)", CollectionGroup.MARVEL, "Higher, further, faster"),
        HeroCategory("deadpool", "Deadpool", "Deadpool", CollectionGroup.MARVEL, "Maximum effort"),
        HeroCategory("wolverine", "Wolverine", "Wolverine (character)", CollectionGroup.MARVEL, "The best there is"),
        HeroCategory("fantastic-four", "Fantastic Four", "Fantastic Four", CollectionGroup.MARVEL, "Marvel's first family"),
        HeroCategory("guardians", "Guardians of the Galaxy", "Guardians of the Galaxy", CollectionGroup.MARVEL, "A bunch of a-holes"),
        HeroCategory("avengers", "Avengers", "Avengers (comics)", CollectionGroup.MARVEL, "Assemble")
    )

    val dc = listOf(
        HeroCategory("batman", "Batman", "Batman", CollectionGroup.DC, "The Dark Knight"),
        HeroCategory("superman", "Superman", "Superman", CollectionGroup.DC, "Man of Tomorrow"),
        HeroCategory("wonder-woman", "Wonder Woman", "Wonder Woman", CollectionGroup.DC, "Amazonian warrior"),
        HeroCategory("flash", "The Flash", "Flash (Barry Allen)", CollectionGroup.DC, "Fastest man alive"),
        HeroCategory("aquaman", "Aquaman", "Aquaman", CollectionGroup.DC, "King of Atlantis"),
        HeroCategory("green-lantern", "Green Lantern", "Green Lantern", CollectionGroup.DC, "In brightest day"),
        HeroCategory("cyborg", "Cyborg", "Cyborg (DC Comics)", CollectionGroup.DC, "Half man, half machine"),
        HeroCategory("supergirl", "Supergirl", "Supergirl", CollectionGroup.DC, "The girl of steel"),
        HeroCategory("nightwing", "Nightwing", "Nightwing", CollectionGroup.DC, "Blüdhaven's protector"),
        HeroCategory("justice-league", "Justice League", "Justice League", CollectionGroup.DC, "The world's greatest heroes")
    )

    val villains = listOf(
        HeroCategory("joker", "Joker", "Joker (character)", CollectionGroup.VILLAINS, "Why so serious?"),
        HeroCategory("thanos", "Thanos", "Thanos", CollectionGroup.VILLAINS, "The mad titan"),
        HeroCategory("loki", "Loki", "Loki (Marvel Comics)", CollectionGroup.VILLAINS, "God of mischief"),
        HeroCategory("venom", "Venom", "Venom (character)", CollectionGroup.VILLAINS, "We are Venom"),
        HeroCategory("green-goblin", "Green Goblin", "Green Goblin", CollectionGroup.VILLAINS, "Norman Osborn"),
        HeroCategory("darkseid", "Darkseid", "Darkseid", CollectionGroup.VILLAINS, "Ruler of Apokolips"),
        HeroCategory("lex-luthor", "Lex Luthor", "Lex Luthor", CollectionGroup.VILLAINS, "Metropolis' greatest threat"),
        HeroCategory("harley-quinn", "Harley Quinn", "Harley Quinn", CollectionGroup.VILLAINS, "Puddin'"),
        HeroCategory("red-skull", "Red Skull", "Red Skull", CollectionGroup.VILLAINS, "Hail HYDRA"),
        HeroCategory("ultron", "Ultron", "Ultron", CollectionGroup.VILLAINS, "There are no strings on me")
    )

    val collections = listOf(
        HeroCategory("superhero-groups", "Superhero Groups", "superhero team group", CollectionGroup.COLLECTIONS, "Teams that fight together"),
        HeroCategory("comic-book-art", "Comic Book Art", "comic book art", CollectionGroup.COLLECTIONS, "Panels, inks and covers"),
        HeroCategory("superhero-logos", "Superhero Logos", "superhero logo", CollectionGroup.COLLECTIONS, "Emblems and insignia"),
        HeroCategory("classic-heroes", "Classic Heroes", "classic comic superhero", CollectionGroup.COLLECTIONS, "Golden and silver age legends"),
        HeroCategory("animated-heroes", "Animated Superheroes", "animated superhero cartoon", CollectionGroup.COLLECTIONS, "Saturday morning legends"),
        HeroCategory("superhero-movies", "Superhero Movies", "superhero movie premiere", CollectionGroup.COLLECTIONS, "Cinema and red carpets"),
        HeroCategory("villain-wallpapers", "Villain Wallpapers", "villain cosplay", CollectionGroup.COLLECTIONS, "Bad guys, great art"),
        HeroCategory("cinematic-art", "Cinematic Artwork", "cinematic superhero", CollectionGroup.COLLECTIONS, "Big screen drama")
    )

    /** Broad query used for the "Latest wallpapers" live feed. */
    const val LIVE_FEED_QUERY = "superhero"

    /** Broad query used to pick the daily wallpaper. */
    const val DAILY_QUERY = "superhero comic"

    /** Whole-universe entry points used by the home screen "See all" actions. */
    val groups = listOf(
        HeroCategory("marvel-all", "Marvel Universe", "Marvel Comics", CollectionGroup.MARVEL, "Earth's mightiest artwork"),
        HeroCategory("dc-all", "DC Universe", "DC Comics", CollectionGroup.DC, "Guardians of the DC multiverse"),
        HeroCategory("villains-all", "Popular Villains", "comic book villain", CollectionGroup.VILLAINS, "The darker side of the panel"),
        HeroCategory("latest-all", "Latest wallpapers", LIVE_FEED_QUERY, CollectionGroup.COLLECTIONS, "Newest uploads on Wikimedia Commons")
    )

    val all: List<HeroCategory> = marvel + dc + villains + collections + groups

    private val byId = all.associateBy { it.id }

    fun byId(id: String): HeroCategory? = byId[id] ?: groups.firstOrNull { it.id == id }

    fun featured(): List<HeroCategory> = listOf("spider-man", "batman", "iron-man", "wonder-woman", "thor", "joker")
        .mapNotNull { byId[it] }

    fun trending(): List<HeroCategory> = listOf("deadpool", "harley-quinn", "venom", "superman", "black-panther", "thanos")
        .mapNotNull { byId[it] }

}
