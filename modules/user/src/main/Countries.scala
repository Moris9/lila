package lila.user

import scala._

object Countries {

  val all = Map(
    "ad" -> "Andorra",
    "ae" -> "United Arab Emirates",
    "af" -> "Afghanistan",
    "ag" -> "Antigua and Barbuda",
    "al" -> "Albania",
    "am" -> "Armenia",
    "ao" -> "Angola",
    "ar" -> "Argentina",
    "at" -> "Austria",
    "au" -> "Australia",
    "az" -> "Azerbaijan",
    "ba" -> "Bosnia-Herzegovina",
    "bb" -> "Barbados",
    "bd" -> "Bangladesh",
    "be" -> "Belgium",
    "bf" -> "Burkina Faso",
    "bg" -> "Bulgaria",
    "bh" -> "Bahrain",
    "bi" -> "Burundi",
    "bj" -> "Benin",
    "bn" -> "Brunei",
    "bo" -> "Bolivia",
    "br" -> "Brazil",
    "bs" -> "Bahamas",
    "bt" -> "Bhutan",
    "bw" -> "Botswana",
    "by" -> "Belarus",
    "bz" -> "Belize",
    "ca" -> "Canada",
    "cd" -> "Congo (Democratic Rep.)",
    "cf" -> "Central African Republic",
    "cg" -> "Congo (Brazzaville)",
    "ch" -> "Switzerland",
    "ci" -> "Cote d'Ivoire",
    "cl" -> "Chile",
    "cm" -> "Cameroon",
    "cn" -> "China",
    "co" -> "Colombia",
    "cr" -> "Costa Rica",
    "cu" -> "Cuba",
    "cv" -> "Cape Verde",
    "cy" -> "Cyprus",
    "cz" -> "Czech Republic",
    "de" -> "Germany",
    "dj" -> "Djibouti",
    "dk" -> "Denmark",
    "dm" -> "Dominica",
    "do" -> "Dominican Republic",
    "dz" -> "Algeria",
    "ec" -> "Ecuador",
    "ee" -> "Estonia",
    "eg" -> "Egypt",
    "eh" -> "Western Sahara",
    "er" -> "Eritrea",
    "es" -> "Spain",
    "et" -> "Ethiopia",
    "fi" -> "Finland",
    "fj" -> "Fiji",
    "fm" -> "Micronesia",
    "fr" -> "France",
    "ga" -> "Gabon",
    "gb" -> "United Kingdom",
    "gd" -> "Grenada",
    "ge" -> "Georgia",
    "gh" -> "Ghana",
    "gl" -> "Greenland",
    "gm" -> "Gambia",
    "gn" -> "Guinea",
    "gq" -> "Equatorial Guinea",
    "gr" -> "Greece",
    "gt" -> "Guatemala",
    "gw" -> "Guinea-Bissau",
    "gy" -> "Guyana",
    "hn" -> "Honduras",
    "hr" -> "Croatia",
    "ht" -> "Haiti",
    "hu" -> "Hungary",
    "id" -> "Indonesia",
    "ie" -> "Ireland",
    "il" -> "Israel",
    "in" -> "India",
    "iq" -> "Iraq",
    "ir" -> "Iran",
    "is" -> "Iceland",
    "it" -> "Italy",
    "jm" -> "Jamaica",
    "jo" -> "Jordan",
    "jp" -> "Japan",
    "ke" -> "Kenya",
    "kg" -> "Kyrgyzstan",
    "kh" -> "Cambodia",
    "ki" -> "Kiribati",
    "km" -> "Comoros",
    "kn" -> "Saint Kitts and Nevis",
    "kp" -> "North Korea",
    "kr" -> "South Korea",
    "kw" -> "Kuwait",
    "ky" -> "Cayman Islands",
    "kz" -> "Kazakhstan",
    "la" -> "Laos",
    "lb" -> "Lebanon",
    "lc" -> "Saint Lucia",
    "li" -> "Liechtenstein",
    "lk" -> "Sri Lanka",
    "lr" -> "Liberia",
    "ls" -> "Lesotho",
    "lt" -> "Lithuania",
    "lu" -> "Luxembourg",
    "lv" -> "Latvia",
    "ly" -> "Libya",
    "ma" -> "Morocco",
    "md" -> "Moldova",
    "me" -> "Montenegro",
    "mg" -> "Madagascar",
    "mh" -> "Marshall Islands",
    "mk" -> "Macedonia",
    "ml" -> "Mali",
    "mm" -> "Myanmar",
    "mn" -> "Mongolia",
    "mr" -> "Mauritania",
    "mt" -> "Malta",
    "mu" -> "Mauritius",
    "mv" -> "Maldives",
    "mw" -> "Malawi",
    "mx" -> "Mexico",
    "my" -> "Malaysia",
    "mz" -> "Mozambique",
    "na" -> "Namibia",
    "ne" -> "Niger",
    "ng" -> "Nigeria",
    "ni" -> "Nicaragua",
    "nl" -> "Netherlands",
    "no" -> "Norway",
    "np" -> "Nepal",
    "nr" -> "Nauru",
    "nz" -> "New Zealand",
    "om" -> "Oman",
    "pa" -> "Panama",
    "pe" -> "Peru",
    "pg" -> "Papua New Guinea",
    "ph" -> "Philippines",
    "pk" -> "Pakistan",
    "pl" -> "Poland",
    "pt" -> "Portugal",
    "pw" -> "Palau",
    "py" -> "Paraguay",
    "qa" -> "Qatar",
    "ro" -> "Romania",
    "rs" -> "Serbia",
    "ru" -> "Russia",
    "rw" -> "Rwanda",
    "sa" -> "Saudi Arabia",
    "sb" -> "Solomon Islands",
    "sc" -> "Seychelles",
    "sd" -> "Sudan",
    "se" -> "Sweden",
    "sg" -> "Singapore",
    "si" -> "Slovenia",
    "sk" -> "Slovakia",
    "sl" -> "Sierra Leone",
    "sm" -> "San Marino",
    "sn" -> "Senegal",
    "so" -> "Somalia",
    "sr" -> "Suriname",
    "st" -> "Sao Tome and Principe",
    "sv" -> "El Salvador",
    "sy" -> "Syria",
    "sz" -> "Swaziland",
    "td" -> "Chad",
    "tg" -> "Togo",
    "th" -> "Thailand",
    "tj" -> "Tajikistan",
    "tl" -> "Timor-Leste",
    "tm" -> "Turkmenistan",
    "tn" -> "Tunisia",
    "to" -> "Tonga",
    "tr" -> "Turkey",
    "tt" -> "Trinidad and Tobago",
    "tv" -> "Tuvalu",
    "tw" -> "Taiwan",
    "tz" -> "Tanzania",
    "ua" -> "Ukraine",
    "ug" -> "Uganda",
    "us" -> "United States",
    "uy" -> "Uruguay",
    "uz" -> "Uzbekistan",
    "vc" -> "Saint Vincent and the Grenadines",
    "ve" -> "Venezuela",
    "vn" -> "Vietnam",
    "vu" -> "Vanuatu",
    "ws" -> "Samoa",
    "ye" -> "Yemen",
    "za" -> "South Africa",
    "zm" -> "Zambia",
    "zw" -> "Zimbabwe",
    "zz" -> "World")
}
