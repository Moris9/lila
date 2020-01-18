package lila.clas

import scala.util.Random

private object NameGenerator {

  def apply(maxSize: Int = 16, triesLeft: Int = 100): Option[String] = {
    val name = anyOf(combinations).map(anyOf).mkString
    if (name.size <= maxSize) name.some
    else if (triesLeft > 0) apply(maxSize, triesLeft - 1)
    else none
  }

  private def anyOf[A](vec: Vector[A]): A =
    vec(Random.between(0, vec.size))

  lazy val combinations = Vector(
    List(colors, animals),
    List(adjectives, animals)
  )

  val colors = Vector(
    "Red",
    "Orange",
    "Yellow",
    "Green",
    "Blue",
    "Purple",
    "Brown",
    "Magenta",
    "Tan",
    "Cyan",
    "Olive",
    "Maroon",
    "Navy",
    "Aqua",
    "Turquoise",
    "Silver",
    "Lime",
    "Teal",
    "Indigo",
    "Violet",
    "Pink",
    "Black",
    "White",
    "Gray"
  )

  val animals = Vector(
    "Aardvark",
    "Alligator",
    "Alpaca",
    "Ant",
    "Antelope",
    "Ape",
    "Armadillo",
    "Baboon",
    "Badger",
    "Bat",
    "Bear",
    "Beaver",
    "Bee",
    "Beetle",
    "Buffalo",
    "Butterfly",
    "Camel",
    "Carabao",
    "Caribou",
    "Cat",
    "Cattle",
    "Cheetah",
    "Chicken",
    "Chimpanzee",
    "Chinchilla",
    "Cicada",
    "Clam",
    "Cod",
    "Coyote",
    "Crab",
    "Cricket",
    "Crow",
    "Deer",
    "Dinosaur",
    "Dog",
    "Dolphin",
    "Dragon",
    "Duck",
    "Eagle",
    "Echidna",
    "Eel",
    "Elephant",
    "Elk",
    "Ferret",
    "Fish",
    "Fly",
    "Fox",
    "Frog",
    "Gerbil",
    "Giraffe",
    "Gnat",
    "Gnu",
    "Goat",
    "Goldfish",
    "Goose",
    "Gorilla",
    "Grasshopper",
    "Guinea",
    "Hamster",
    "Hare",
    "Hedgehog",
    "Herring",
    "Hippopotamus",
    "Hornet",
    "Horse",
    "Hound",
    "Human",
    "House",
    "Hyena",
    "Impala",
    "Insect",
    "Jackal",
    "Jellyfish",
    "Kangaroo",
    "Koala",
    "Leopard",
    "Lion",
    "Lizard",
    "Llama",
    "Locust",
    "Louse",
    "Macaw",
    "Mallard",
    "Mammoth",
    "Manatee",
    "Marten",
    "Mink",
    "Minnow",
    "Mole",
    "Monkey",
    "Moose",
    "Mosquito",
    "Mouse",
    "Mule",
    "Muskrat",
    "Otter",
    "Ox",
    "Oyster",
    "Panda",
    "Pig",
    "Platypus",
    "Porcupine",
    "Prairie",
    "Pug",
    "Rabbit",
    "Raccoon",
    "Reindeer",
    "Rhinoceros",
    "Salmon",
    "Sardine",
    "Scorpion",
    "Seal",
    "Serval",
    "Shark",
    "Sheep",
    "Skunk",
    "Snail",
    "Snake",
    "Spider",
    "Squirrel",
    "Swan",
    "Termite",
    "Tiger",
    "Trout",
    "Turtle",
    "Walrus",
    "Wasp",
    "Weasel",
    "Whale",
    "Wolf",
    "Wombat",
    "Woodchuck",
    "Worm",
    "Yak",
    "Yellowjacket",
    "Zebra"
  )

  val adjectives = Vector(
    "Abiding",
    "Able",
    "Absolute",
    "Absolved",
    "Abundant",
    "Academic",
    "Accepted",
    "Accurate",
    "Active",
    "Actual",
    "Acute",
    "Adamant",
    "Adept",
    "Adequate",
    "Adjusted",
    "Admired",
    "Adonic",
    "Adorable",
    "Adored",
    "Adroit",
    "Advanced",
    "Affable",
    "Affined",
    "Affluent",
    "Ageless",
    "Agile",
    "Aholic",
    "Alert",
    "Alive",
    "Allied",
    "Alluring",
    "Alright",
    "Amative",
    "Amatory",
    "Amazing",
    "Amenable",
    "Amiable",
    "Amicable",
    "Amusing",
    "Angelic",
    "Aplenty",
    "Appetent",
    "Apposite",
    "Apropos",
    "Apt",
    "Ardent",
    "Artistic",
    "Aspirant",
    "Aspiring",
    "Assured",
    "Assuring",
    "Astir",
    "Astute",
    "Athletic",
    "Atypical",
    "August",
    "Avid",
    "Awaited",
    "Awake",
    "Aware",
    "Awash",
    "Awesome",
    "Balanced",
    "Baronial",
    "Beaming",
    "Beatific",
    "Becoming",
    "Beefy",
    "Beloved",
    "Benefic",
    "Benign",
    "Best",
    "Better",
    "Big",
    "Biggest",
    "Bijou",
    "Blazing",
    "Blessed",
    "Blissful",
    "Blithe",
    "Blooming",
    "Bold",
    "Bonny",
    "Bonzer",
    "Boss",
    "Bound",
    "Brainy",
    "Brave",
    "Brawny",
    "Breezy",
    "Brief",
    "Bright",
    "Brill",
    "Brimming",
    "Brisk",
    "Bubbly",
    "Budding",
    "Buff",
    "Bullish",
    "Buoyant",
    "Bustling",
    "Busy",
    "Buxom",
    "Calm",
    "Calming",
    "Canny",
    "Canty",
    "Capable",
    "Capital",
    "Carefree",
    "Careful",
    "Caring",
    "Casual",
    "Centered",
    "Central",
    "Cerebral",
    "Certain",
    "Champion",
    "Charming",
    "Cheerful",
    "Cherry",
    "Chic",
    "Chipper",
    "Chirpy",
    "Choice",
    "Chosen",
    "Chummy",
    "Civic",
    "Civil",
    "Classic",
    "Classy",
    "Clean",
    "Clear",
    "Clement",
    "Clever",
    "Close",
    "Clubby",
    "Coequal",
    "Cogent",
    "Coherent",
    "Colossal",
    "Coltish",
    "Comely",
    "Comic",
    "Comical",
    "Complete",
    "Composed",
    "Concise",
    "Concrete",
    "Constant",
    "Content",
    "Cool",
    "Copious",
    "Cordial",
    "Correct",
    "Cosmic",
    "Cosy",
    "Courtly",
    "Cozy",
    "Creamy",
    "Creative",
    "Credible",
    "Crisp",
    "Crucial",
    "Cuddly",
    "Cultured",
    "Cunning",
    "Curious",
    "Current",
    "Cushy",
    "Cute",
    "Dainty",
    "Dandy",
    "Dapper",
    "Daring",
    "Darling",
    "Dashing",
    "Dazzling",
    "Dear",
    "Debonair",
    "Decent",
    "Deciding",
    "Decisive",
    "Decorous",
    "Deep",
    "Defiant",
    "Definite",
    "Deft",
    "Delicate",
    "Deluxe",
    "Designer",
    "Desired",
    "Desirous",
    "Destined",
    "Devoted",
    "Devout",
    "Didactic",
    "Diligent",
    "Dinkum",
    "Direct",
    "Discreet",
    "Discrete",
    "Distinct",
    "Diverse",
    "Divine",
    "Doable",
    "Dominant",
    "Doting",
    "Doughty",
    "Dreamy",
    "Driven",
    "Driving",
    "Durable",
    "Dutiful",
    "Dynamic",
    "Dynamite",
    "Eager",
    "Early",
    "Earnest",
    "Earthly",
    "Earthy",
    "Easy",
    "Eclectic",
    "Economic",
    "Ecstatic",
    "Edified",
    "Educated",
    "Elated",
    "Elating",
    "Elder",
    "Electric",
    "Elegant",
    "Eligible",
    "Eloquent",
    "Emerging",
    "Eminent",
    "Enamored",
    "Enduring",
    "Engaging",
    "Enhanced",
    "Enormous",
    "Enough",
    "Enticing",
    "Equable",
    "Equal",
    "Equipped",
    "Erotic",
    "Erudite",
    "Especial",
    "Esteemed",
    "Esthetic",
    "Eternal",
    "Ethical",
    "Euphoric",
    "Eventful",
    "Evident",
    "Exact",
    "Exalted",
    "Exotic",
    "Exultant",
    "Fab",
    "Fabulous",
    "Facile",
    "Factual",
    "Fain",
    "Fair",
    "Faithful",
    "Famed",
    "Familial",
    "Familiar",
    "Family",
    "Famous",
    "Fancy",
    "Fast",
    "Favored",
    "Favorite",
    "Fearless",
    "Feasible",
    "Fecund",
    "Fertile",
    "Fervent",
    "Festal",
    "Festive",
    "Fetching",
    "Fiery",
    "Fine",
    "Finer",
    "Finest",
    "Firm",
    "First",
    "Fit",
    "Fitting",
    "Flash",
    "Flashy",
    "Flawless",
    "Fleet",
    "Flexible",
    "Fluent",
    "Flying",
    "Focused",
    "Fond",
    "Forceful",
    "Foremost",
    "Forward",
    "Foxy",
    "Fragrant",
    "Frank",
    "Free",
    "Freely",
    "Fresh",
    "Friendly",
    "Frisky",
    "Fruitful",
    "Full",
    "Fun",
    "Funny",
    "Gainful",
    "Gallant",
    "Galore",
    "Game",
    "Gamesome",
    "Generous",
    "Genial",
    "Genteel",
    "Gentle",
    "Genuine",
    "Germane",
    "Gettable",
    "Giddy",
    "Gifted",
    "Giving",
    "Glad",
    "Gleaming",
    "Gleeful",
    "Glorious",
    "Glowing",
    "Gnarly",
    "Godly",
    "Golden",
    "Good",
    "Goodly",
    "Gorgeous",
    "Graced",
    "Graceful",
    "Gracile",
    "Gracious",
    "Gradely",
    "Graithly",
    "Grand",
    "Grateful",
    "Great",
    "Greatest",
    "Groovy",
    "Grounded",
    "Growing",
    "Grown",
    "Guided",
    "Guiding",
    "Gutsy",
    "Halcyon",
    "Hale",
    "Hallowed",
    "Handsome",
    "Handy",
    "Happy",
    "Hardy",
    "Harmless",
    "Head",
    "Healing",
    "Healthy",
    "Hearty",
    "Heavenly",
    "Heedful",
    "Helpful",
    "Hep",
    "Heralded",
    "Heroic",
    "High",
    "Highest",
    "Hip",
    "Holy",
    "Homely",
    "Honest",
    "Honeyed",
    "Honorary",
    "Honored",
    "Hopeful",
    "Hot",
    "Hotshot",
    "Huggy",
    "Humane",
    "Humble",
    "Humorous",
    "Hunky",
    "Hygienic",
    "Hypnotic",
    "Ideal",
    "Idolized",
    "Imitable",
    "Immense",
    "Immortal",
    "Immune",
    "Impish",
    "Improved",
    "In",
    "Incisive",
    "Included",
    "Inerrant",
    "Infant",
    "Infinite",
    "Informed",
    "Initiate",
    "Innocent",
    "Inspired",
    "Integral",
    "Intense",
    "Intent",
    "Internal",
    "Intimate",
    "Intrepid",
    "Inviting",
    "Jaunty",
    "Jazzed",
    "Jazzy",
    "Jessant",
    "Jestful",
    "Jesting",
    "Jewelled",
    "Jiggish",
    "Jigjog",
    "Jimp",
    "Jobbing",
    "Jocose",
    "Jocular",
    "Jocund",
    "Joint",
    "Jointed",
    "Jolif",
    "Jolly",
    "Jovial",
    "Joyful",
    "Joyous",
    "Joysome",
    "Jubilant",
    "Juicy",
    "Jump",
    "Just",
    "Keen",
    "Kempt",
    "Key",
    "Kind",
    "Kindly",
    "Kindred",
    "Kinetic",
    "Kingly",
    "Kissable",
    "Knightly",
    "Knowable",
    "Knowing",
    "Kooky",
    "Kosher",
    "Ladylike",
    "Large",
    "Lasting",
    "Laudable",
    "Laureate",
    "Lavish",
    "Lawful",
    "Leading",
    "Learned",
    "Legal",
    "Legible",
    "Legit",
    "Leisured",
    "Lenien",
    "Leonine",
    "Lepid",
    "Lettered",
    "Liberal",
    "Lightly",
    "Likable",
    "Like",
    "Liked",
    "Likely",
    "Limber",
    "Literary",
    "Literate",
    "Lithe",
    "Live",
    "Lively",
    "Logical",
    "Lordly",
    "Lovable",
    "Loved",
    "Lovely",
    "Loving",
    "Loyal",
    "Lucent",
    "Lucid",
    "Lucky",
    "Luminous",
    "Luscious",
    "Lush",
    "Lustrous",
    "Lusty",
    "Made",
    "Magical",
    "Magnetic",
    "Maiden",
    "Main",
    "Majestic",
    "Major",
    "Manifest",
    "Manly",
    "Mannerly",
    "Many",
    "Marked",
    "Master",
    "Masterly",
    "Maternal",
    "Mature",
    "Maturing",
    "Maximal",
    "Mediate",
    "Meek",
    "Mellow",
    "Merciful",
    "Merry",
    "Meteoric",
    "Mighty",
    "Mindful",
    "Minikin",
    "Mint",
    "Mirthful",
    "Model",
    "Modern",
    "Modest",
    "Moneyed",
    "Moral",
    "More",
    "Most",
    "Mother",
    "Motor",
    "Moving",
    "Much",
    "Mucho",
    "Muscular",
    "Musical",
    "Must",
    "Mutual",
    "National",
    "Native",
    "Natty",
    "Natural",
    "Nearby",
    "Neat",
    "Needed",
    "Neoteric",
    "Nestling",
    "New",
    "Newborn",
    "Nice",
    "Nifty",
    "Nimble",
    "Nippy",
    "Noble",
    "Noetic",
    "Normal",
    "Notable",
    "Noted",
    "Novel",
    "Now",
    "Nubile",
    "Obliging",
    "Official",
    "OK",
    "Okay",
    "Olympian",
    "On",
    "Once",
    "One",
    "Onward",
    "Open",
    "Optimal",
    "Optimum",
    "Opulent",
    "Orderly",
    "Organic",
    "Oriented",
    "Original",
    "Outgoing",
    "Overt",
    "Pally",
    "Palpable",
    "Parental",
    "Partisan",
    "Paternal",
    "Patient",
    "Peaceful",
    "Peachy",
    "Peerless",
    "Peppy",
    "Perfect",
    "Perky",
    "Pert",
    "Pet",
    "Petite",
    "Picked",
    "Pierian",
    "Pilot",
    "Pious",
    "Piquant",
    "Pithy",
    "Pivotal",
    "Placid",
    "Playful",
    "Pleasant",
    "Pleased",
    "Pleasing",
    "Plenary",
    "Plenty",
    "Pliable",
    "Plucky",
    "Plummy",
    "Plus",
    "Plush",
    "Poetic",
    "Poignant",
    "Poised",
    "Polished",
    "Polite",
    "Popular",
    "Posh",
    "Positive",
    "Possible",
    "Potent",
    "Powerful",
    "Precious",
    "Precise",
    "Premier",
    "Premium",
    "Prepared",
    "Present",
    "Pretty",
    "Primal",
    "Primary",
    "Prime",
    "Primed",
    "Primo",
    "Princely",
    "Pristine",
    "Prize",
    "Prized",
    "Pro",
    "Probable",
    "Profound",
    "Profuse",
    "Prolific",
    "Prompt",
    "Proper",
    "Protean",
    "Proud",
    "Prudent",
    "Puissant",
    "Pukka",
    "Punchy",
    "Punctual",
    "Pure",
    "Quaint",
    "Quality",
    "Queenly",
    "Quemeful",
    "Quick",
    "Quiet",
    "Quirky",
    "Quiver",
    "Quixotic",
    "Quotable",
    "Racy",
    "Rad",
    "Radiant",
    "Rapid",
    "Rational",
    "Ready",
    "Real",
    "Refined",
    "Regal",
    "Regnant",
    "Regular",
    "Relaxed",
    "Relevant",
    "Reliable",
    "Relieved",
    "Renowned",
    "Resolute",
    "Resolved",
    "Restful",
    "Revered",
    "Reverent",
    "Rich",
    "Right",
    "Rightful",
    "Risible",
    "Robust",
    "Romantic",
    "Rooted",
    "Rosy",
    "Round",
    "Rounded",
    "Rousing",
    "Rugged",
    "Ruling",
    "Sacred",
    "Safe",
    "Sage",
    "Saintly",
    "Salient",
    "Salutary",
    "Sanguine",
    "Sapid",
    "Sapient",
    "Sassy",
    "Saucy",
    "Saving",
    "Savory",
    "Savvy",
    "Scenic",
    "Seamless",
    "Seasonal",
    "Seasoned",
    "Secure",
    "Sedulous",
    "Seemly",
    "Select",
    "Selfless",
    "Sensible",
    "Sensual",
    "Sensuous",
    "Serene",
    "Service",
    "Set",
    "Settled",
    "Shapely",
    "Sharp",
    "Sheen",
    "Shining",
    "Shiny",
    "Showy",
    "Shrewd",
    "Sightly",
    "Silken",
    "Silky",
    "Silver",
    "Silvery",
    "Simple",
    "Sincere",
    "Sinewy",
    "Singular",
    "Sisterly",
    "Sizable",
    "Sizzling",
    "Skillful",
    "Skilled",
    "Sleek",
    "Slick",
    "Slinky",
    "Smacking",
    "Smart",
    "Smashing",
    "Smiley",
    "Smooth",
    "Snap",
    "Snappy",
    "Snazzy",
    "Snod",
    "Snug",
    "Soaring",
    "Sociable",
    "Social",
    "Societal",
    "Soft",
    "Soigne",
    "Solid",
    "Sonsy",
    "Sooth",
    "Soothing",
    "Soulful",
    "Sound",
    "Spacious",
    "Spangly",
    "Spanking",
    "Sparkly",
    "Special",
    "Specular",
    "Speedy",
    "Spicy",
    "Spiffy",
    "Spirited",
    "Splendid",
    "Sport",
    "Sporting",
    "Sportive",
    "Sporty",
    "Spot",
    "Spotless",
    "Spruce",
    "Spry",
    "Spunky",
    "Square",
    "Stable",
    "Stacked",
    "Stalwart",
    "Staminal",
    "Standard",
    "Standing",
    "Star",
    "Starry",
    "State",
    "Stately",
    "Staunch",
    "Steady",
    "Steamy",
    "Stellar",
    "Sterling",
    "Sthenic",
    "Stirred",
    "Stirring",
    "Stocky",
    "Stoical",
    "Storied",
    "Stout",
    "Striking",
    "Strong",
    "Studious",
    "Stunning",
    "Sturdy",
    "Stylish",
    "Suasive",
    "Suave",
    "Sublime",
    "Substant",
    "Subtle",
    "Succinct",
    "Sugary",
    "Suitable",
    "Sultry",
    "Summary",
    "Summery",
    "Sunny",
    "Super",
    "Superb",
    "Superior",
    "Supernal",
    "Supple",
    "Supreme",
    "Sure",
    "Svelte",
    "Swank",
    "Sweet",
    "Swell",
    "Swift",
    "Swish",
    "Sylvan",
    "Tactful",
    "Talented",
    "Tangible",
    "Tasteful",
    "Tasty",
    "Teeming",
    "Tempean",
    "Tenable",
    "Tender",
    "Terrific",
    "Thankful",
    "Thorough",
    "Thrilled",
    "Thriving",
    "Tidy",
    "Tight",
    "Timeless",
    "Timely",
    "Tiptop",
    "Tireless",
    "Titanic",
    "Today",
    "Together",
    "Tolerant",
    "Top",
    "Tops",
    "Total",
    "Touching",
    "Tough",
    "Tranquil",
    "Traveled",
    "Tretis",
    "Trim",
    "True",
    "Trustful",
    "Trusting",
    "Trusty",
    "Truthful",
    "Tubular",
    "Tuneful",
    "Tympanic",
    "Uber",
    "Ultimate",
    "Ultra",
    "Unafraid",
    "Unbeaten",
    "Unbiased",
    "Unbroken",
    "Uncommon",
    "Unerring",
    "Unharmed",
    "Unhurt",
    "Unified",
    "Unique",
    "United",
    "Unshaken",
    "Unspoilt",
    "Untiring",
    "Unusual",
    "Up",
    "Upbeat",
    "Upcoming",
    "Uplifted",
    "Upright",
    "Upward",
    "Upwardly",
    "Urbane",
    "Usable",
    "Useful",
    "Utmost",
    "Valiant",
    "Valid",
    "Valorous",
    "Valuable",
    "Valued",
    "Vast",
    "Vaulting",
    "Vehement",
    "Venust",
    "Verified",
    "Versed",
    "Very",
    "Vestal",
    "Veteran",
    "Viable",
    "Vibrant",
    "Victor",
    "Vigilant",
    "Vigorous",
    "Virile",
    "Virtuous",
    "Vital",
    "Vivid",
    "Vocal",
    "Volant",
    "Wanted",
    "Warm",
    "Wealthy",
    "Weighty",
    "Welcome",
    "Welcomed",
    "Weleful",
    "Well",
    "Welsome",
    "Whole",
    "Whopping",
    "Willed",
    "Willing",
    "Winged",
    "Winning",
    "Winsome",
    "Wired",
    "Wise",
    "Witty",
    "Wizard",
    "Wizardly",
    "Won",
    "Wondrous",
    "Workable",
    "Worldly",
    "Worth",
    "Worthy",
    "Xenial",
    "Yern",
    "Young",
    "Youthful",
    "Yummy",
    "Zaftig",
    "Zany",
    "Zappy",
    "Zazzy",
    "Zealed",
    "Zealful",
    "Zealous",
    "Zestful",
    "Zesty",
    "Zingy",
    "Zippy",
    "Zooty"
  )
}
