package lila

import lila.socket.WithSocket

package object tournament extends PackageObject with WithPlay with WithSocket {

  private[tournament]type Players = List[tournament.Player]

  private[tournament]type RankedPlayers = List[RankedPlayer]

  private[tournament]type Pairings = List[tournament.Pairing]

  private[tournament]type Events = List[tournament.Event]

  private[tournament] object RandomName {

    private val names = IndexedSeq("Abbot", "Abonyi", "Abrosimov", "Adams", "Addison", "Adorján", "Ahues", "Aitken", "Akobian", "Akopian", "Alapin", "Alberoni", "Albin", "Alekhine", "Alekseev", "Alexander", "Alexandria", "Allgaier", "Almási", "Altschul", "Anand", "Anderssen", "Andersson", "Andreikin", "Angantysson", "Antoshin", "Arakhamia", "Aronian", "Asztalos", "Aulia", "Averbakh", "Bacrot", "Balashov", "Balla", "Barcza", "Barnes", "Barry", "Bartolović", "Bartrina", "Becerra", "Becker", "Behting", "Belavenets", "Beliavsky", "Benko", "Benoit", "Berger", "Bernstein", "Bird", "Birnov", "Biyiasas", "Blackburne", "Bledow", "Blumenfeld", "Boden", "Bogoljubov", "Bohatyrchuk", "Boi", "Bolbochán", "Boleslavsky", "Bondarevsky", "Book", "Botterill", "Botvinnik", "Braga", "Breyer", "Brinck-Claussen", "Brond", "Bronstein", "Browne", "Bruzón", "Buckle", "Burgess", "Burn", "Bykova", "Byrne", "Canal", "Capablanca", "Cardanus", "Carlsen", "Caro", "Caruana", "Casper", "Chajes", "Charousek", "Chen", "Chernev", "Chéron", "Chiburdanidze", "Chigorin", "Christiansen", "Ciocâltea", "Cochrane", "Cohn", "Colle", "Cook", "Cortlever", "Cozio", "Cvitan", "Czerniak", "Damant", "Damiano", "Damjanović", "de Firmian", "del Corral", "del Rio", "de Lucena", "Dely", "Denker", "Denys", "de Rivière", "Deschapelles", "de Segura", "de Vere", "di Bona", "Dickinson", "Ding", "Dolmatov", "Domínguez", "Donner", "Dreev", "Dubois", "Dufresne", "Durao", "Duras", "Durka", "Duz-Khotimirsky", "Dvoiris", "Dvoretsky", "Dzindzichashvili", "Ehlvest", "Eliskases", "Eljanov", "Enevoldsen", "Englisch", "Erenburg", "Estrin", "Euwe", "Evans", "Fahrni", "Falkbeer", "Fancy", "Faragó", "Fatalibekova", "Fawcett", "Feigin", "Felgaer", "Filip", "Fine", "Fischer", "Fitzgerald", "Fitzpatrick", "Flamberg", "Flesch", "Flohr", "Florian", "Foltys", "Forgács", "Forintos", "Forsyth", "Fressinet", "Ftacnik", "Fuchs", "Fuderer", "Furman", "Fyfe", "Gaprindashvili", "García", "Gardener", "Garraway", "Gashimov", "Gavriel", "Gelashvili", "Gelfand", "Geller", "Georgiev", "Gheorghiu", "Ghulam-Kassim", "Gibaud", "Gilg", "Gipslis", "Giri", "Gligorić", "Golmayo", "Golombek", "Grachev", "Grahn", "Greco", "Grischuk", "Grob", "Grünfeld", "Gufeld", "Guimard", "Gulko", "Gunina", "Gunsberg", "Gurgenidze", "Gurieli", "Gurvich", "Gustafsson", "Haast", "Hall", "Hamppe", "Hansen", "Harika", "Harikrishna", "Harmonist", "Hartston", "Harwitz", "Havasi", "Heathcote", "Heisman", "Herb", "Herbstman", "Hodges", "Holmov", "Holt", "Honfi", "Horner", "Horowitz", "Hort", "Hou", "Howe", "Howell", "Hromádka", "Huang", "Hübner", "Hug", "Hulak", "Ilyin-Genevsky", "Immonen", "Inarkiev", "Ioseliani", "Ivanchuk", "Ivanov", "Ivkov", "Jaffé", "Jakovenko", "James", "Janowski", "Jefferson", "Jennings", "Jobava", "Johansen", "Johansson", "Johner", "Ju", "Junge", "Kachiani", "Kahn", "Kamsky", "Kan", "Karasev", "Karjakin", "Karlsson", "Karpov", "Kashdan", "Kashimdzanov", "Kasimdzhanov", "Kasparov", "Kasparyan", "Kavalek", "Kazantsev", "Keating", "Keres", "Khalifman", "Kieseritzky", "Kindermann", "King", "Kipping", "Kiriakov", "Klee", "Klein", "Klímová-Richtrová", "Kling", "Klyukin", "Kmoch", "Koblencs", "Koch", "Koltanowski", "Kopaev", "Koppel", "Korchnoi", "Korobov", "Korolkov", "Kosintseva", "Kosteniuk", "Kostić", "Kotov", "Kovalevskaya", "Kozlovskaya", "Kozul", "Kramnik", "Krejcik", "Krogius", "Kubbel", "Kudrin", "Kunde", "Kupchik", "Kupreichik", "Kurajica", "Kushnir", "Kuzmin", "Kuznetsov", "La Bourdonnais", "Lamb", "Landa", "Lange", "Larsen", "Lasker", "Lautier", "Lawrence", "Lazard", "Lazarević", "Laznicka", "Lechtynsky", "Lee", "Leko", "Lematchko", "Lengyel", "Leonhardt", "Letelier", "Levenfish", "Levenfish", "Levitina", "Levitsky", "Lewis", "Li", "Liberzon", "Liburkin", "Lilienthal", "Lipke", "Liren", "Lisitsin", "Litinskaya-Shul", "Ljubojević", "Lolli", "Lombardy", "Lommer", "Loncar", "López", "Lovrić", "Löwenthal", "Loyd", "Lundin", "Mackenzie", "Maiorov", "Malakhov", "Malet", "Mamedyarov", "Manta", "Marache", "Marco", "Marić", "Mariotti", "Maróczy", "Marshall", "Mason", "Matanović", "Matlakov", "Mattisons", "Matulović", "Matveeva", "Maurian", "Mayer", "Mayet", "McDonnell", "McShane", "Mecking", "Medina", "Meier", "Menchik", "Michell", "Mieses", "Mikenas", "Miles", "Mista", "Mongredien", "Montuoro", "Morozevich", "Morphy", "Motylev", "Movsesian", "Murray", "Muzio", "Muzychuk", "Naiditsch", "Najdorf", "Najer", "Nakamura", "Napier", "Navara", "Nedeljković", "Negi", "Nei", "Nemet", "Nenarokov", "Nepomniachtchi", "Neumann", "Ni", "Nicholson", "Nikitin", "Nikolić", "Nimzowitsch", "Nogueiras", "Nunn", "Nyholm", "Nyman", "Nyzhnik", "O'Kelly", "Ólafsson", "Olland", "Onischuk", "Opocensky", "Ortega", "Ortueta", "Osnos", "Owen", "Pachl", "Pachman", "Padevsky", "Palac", "Palda", "Palme", "Panno", "Parma", "Parr", "Paulsen", "Pavasovic", "Pavelka", "Peachey", "Pelletier", "Peng", "Penrose", "Pereira", "Perlis", "Petrosian", "Petrov", "Petrović", "Philidor", "Phillips", "Piacenza (check)", "Pierrot", "Pietzsch", "Piket", "Pillsbury", "Pilnik", "Pirc", "Pirogov", "Plachetka", "Planinc", "Pogonina", "Polerio", "Polgár", "Pollock", "Polugaevsky", "Pomar", "Ponomariov", "Ponziani", "Porges", "Porreca", "Portisch", "Prins", "Prokes", "Przepiórka", "Puc", "Purdy", "Pytel", "Rabar", "Rabinovich", "Radjabov", "Radulov", "Ragozin", "Ramírez", "Ranken", "Rapport", "Rauzer", "Razuvaev", "Reggio", "Reinfeld", "Rellstab", "Reshevsky", "Réti", "Ribli", "Rice", "Richter", "Rinck", "Riumin", "Robatsch", "Rohde", "Romanishin", "Romanov", "Romanovsky", "Rosanes", "Rosenthal", "Rosselli", "Rossetto", "Rossolimo", "Rotlewi", "Ruan", "Rubinstein", "Rublevsky", "Rubtsova", "Rudenko", "Sahović", "Saint-Amant", "Salov", "Salvio", "Salwe", "Sämisch", "Sandipan", "Sanguineti", "Santasiere", "Sarapu", "Sargissian", "Sarić", "Sarratt", "Sax", "Schallopp", "Schiffers", "Schlechter", "Schmid", "Schories", "Schulten", "Sebag", "Seirawan", "Selenus", "Selezniev", "Semenova", "Sergeant", "Sergievsky", "Shabalov", "Sherrard", "Shipley", "Shirov", "Short", "Showalter", "Shulman", "Shumov", "Sigurjónsson", "Silman", "Simagin", "Sjugirov", "Skripchenko", "Smeets", "Smejkal", "Smirin", "Smyslov", "So", "Sokolov", "Sokolsky", "Soltis", "Sosonko", "Sozin", "Spassky", "Speelman", "Spielmann", "Ståhlberg", "Stamma", "Stanchev", "Stanley", "Starr", "Staunton", "Stean", "Stefanova", "Stefanović", "Stein", "Steiner", "Steinitz", "Stocchi", "Stoltz", "Strautins", "Süchting", "Suetin", "Suhle", "Sultan Khan", "Sutovsky", "Suttles", "Sveshnikov", "Svidler", "Swiderski", "Szabó", "Szén", "Taimanov", "Takács", "Tal", "Tan", "Tarjan", "Tarrasch", "Tartakower", "Taubenhaus", "Taylor", "Teichmann", "Teschner", "Thirring", "Thomas", "Timman", "Tiviakov", "Tolush", "Tomashevski", "Topalov", "Torre", "Trenchard", "Trexler", "Treybal", "Trifunović", "Tringov", "Troitsky", "Udovcić", "Uhlmann", "Ujtelky", "Ulrich", "Ulvestad", "Unzicker", "Urzica", "Ushenina", "Vachier-Lagrave", "Vaganyan", "Vallejo Pons", "van den Berg", "van den Bosch", "van Scheltinga", "van Wely", "Vasiliev", "Vasiukov", "Velimirović", "Veresov", "Verlinsky", "Vidmar", "Vitiugov", "Volpert", "von Bardeleben", "von Bilguer", "von der Lasa", "von Gottschall", "von Kolisch", "von Popiel", "Votruba", "Vuković", "Wade", "Wagner", "Waitzkin", "Walbrodt", "Walker", "Walter", "Wang", "Webb", "Wei", "Weiss", "Werle", "Westerinen", "Whitaker", "White", "Williams", "Winawer", "Winter", "Wisker", "Wittek", "Wojtaszek", "Wolff", "Wood", "Wyvill", "Xie", "Xu", "Yanofsky", "Yates", "Young", "Yurevich", "Yusupov", "Zagorovsky", "Zelcic", "Zhao", "Zhukova", "Zinn", "Znosko-Borovsky", "Zubarev", "Zuckerman", "Zukertort", "Zvorykina", "Żytogórski") // do not add characters from this list: https://en.wikipedia.org/wiki/ISO/IEC_8859-1#Languages_commonly_supported_but_with_incomplete_coverage
    private val size = names.size

    def apply(): String = names(scala.util.Random nextInt size)
  }
}

package tournament {

case class RankedPlayer(rank: Int, player: Player) {
  def is(other: RankedPlayer) = player is other.player
}

case class Winner(tourId: String, tourName: String, userId: String)

private[tournament] case class AllUserIds(all: List[String], waiting: List[String])
}
