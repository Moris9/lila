package lila.shutup

/**
 * - words are automatically pluralized. "tit" will also match "tits"
 * - words are automatically leetified. "tit" will also match "t1t", "t-i-t", and more.
 * - words do not partial match. "anal" will NOT match "analysis".
 */
object Dictionary {

  val en: List[String] = (enBase ++ enUk ++ enUs).distinct

  private def enBase = dict("""
anal
anus
ass
asshole
bastard
bitch
boob
cock
coward
cum
cunnilingu
cunt
cunting
dick
dildo
dyke
fack
fag
faggot
fuck
fucking
fuckstick
fucktard
fuk
handjob
homo
incest
jerk
jizz
kike
kunt
muff
niger
nigger
pederast
penis
piss
poop
pussy
queer
rape
retard
retarded
scum
scumbag
semen
sex
shit
shitbag
shite
shitty
shity
slut
titties
twat
vagin
vagina
vulva
wank
""").pp
  private def enUk = dict("""
analplug
analsex
arse
arsehole
balls
bimbo
blowjob
bollocks
boner
boobies
boobs
bugger
bukkake
bullshit
chink
clit
clitoris
cocksucker
condom
coon
crap
cumshot
damm
dammit
damn
dickhead
doggystyle
f0ck
fags
fanny
fck
fcker
fckr
fcku
fcuk
fucked
fucker
fuckface
fuckr
fuct
genital
genitalia
genitals
glory hole
gloryhole
gobshite
godammet
godammit
goddammet
goddammit
goddamn
gypo
hitler
hooker
hore
horny
jesussucks
jizzum
kaffir
kill
killer
killin
killing
lesbo
masturbate
milf
molest
moron
motherfuck
mthrfckr
murder
murderer
nazi
negro
niga
nigah
nigga
niggah
nonce
paedo
paedophile
paki
pecker
pedo
pedofile
pedophile
phuk
pig
pimp
poof
porn
prick
pron
prostitute
raped
rapes
rapist
schlong
screw
scrotum
shag
shemale
shite
shiz
slag
spastic
spaz
sperm
spunk
stripper
stupid
tart
terrorist
tit
tittyfuck
tosser
turd
vaginal
vibrator
wanker
weed
wetback
whor
whore
wog
wtf
xxx
""")
  private def enUs = dict("""
abortion
anus
bewb
blow
blumpkin
cawk
choad
cooter
cornhole
dong
douche
fart
foreskin
gangbang
gook
hell
honkey
humping
jiz
labia
nutsack
pen1s
poon
punani
queef
quim
rectal
rectum
rimjob
spick
spoo
spooge
taint
titty
vag
whore
""")

  private def dict(words: String) = words.lines.filter(_.nonEmpty).toList
}
