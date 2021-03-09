package no.nav.familie.ba.soknad.api.personopplysning

import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import org.springframework.stereotype.Service

@Service
class PersonopplysningerService(
    private val pdlClient: PdlClient,
    private val barnePdlClient: BarnePdlClient
) {

    private fun assertUgradertAdresse(adresseBeskyttelse: List<Adressebeskyttelse>) {
        if (adresseBeskyttelse.any { it.gradering != ADRESSEBESKYTTELSEGRADERING.UGRADERT }) {
            throw GradertAdresseException()
        }
    }

    private fun hentBarn(personIdent: String): HentBarnResponse {
        val response = barnePdlClient.hentBarn(personIdent)
        return Result.runCatching {
            val adresseBeskyttelse = response.data.person!!.adressebeskyttelse
            assertUgradertAdresse(adresseBeskyttelse)

            HentBarnResponse(
                navn = response.data.person.navn.first().fulltNavn(),
                fødselsdato = response.data.person.foedsel.first().foedselsdato!!,
                adresse = response.data.person.bostedsadresse.firstOrNull()
            )
        }.fold(
            onSuccess = { it },
            onFailure = { throw it }
        )
    }

    fun borMedSøker(søkerAdresse: Bostedsadresse?, barneAdresse: Bostedsadresse?): Boolean {
        fun adresseListe(bostedsadresse: Bostedsadresse): List<Any?> {
            return listOfNotNull(bostedsadresse.matrikkeladresse, bostedsadresse.vegadresse)
        }

        return if (søkerAdresse == null || barneAdresse == null) false
        else {
            val søkerAdresser = adresseListe(søkerAdresse)
            val barneAdresser = adresseListe(barneAdresse)
            søkerAdresser.any { barneAdresser.contains(it) }
        }
    }

    fun hentPersoninfo(personIdent: String): Person {
        val response = pdlClient.hentSøker(personIdent)
        return Result.runCatching {
            val adresseBeskyttelse = response.data.person!!.adressebeskyttelse
            assertUgradertAdresse(adresseBeskyttelse)

            val statsborgerskap: List<Statborgerskap> = mapStatsborgerskap(response.data.person.statsborgerskap)
            val barn: Set<Barn> = mapBarn(response.data.person)
            val sivilstandType = mapSivilstandType(response.data.person.sivilstand)
            val adresse = mapAdresser(response.data.person.bostedsadresse.firstOrNull())

            response.data.person.let {
                Person(
                    ident = personIdent,
                    navn = it.navn.first().fulltNavn(),
                    statsborgerskap = statsborgerskap,
                    barn = barn,
                    siviltstatus = Sivilstand(sivilstandType),
                    adresse = adresse
                )
            }
        }.fold(
            onSuccess = { it },
            onFailure = { throw it }
        )
    }

    private fun mapAdresser(bostedsadresse: Bostedsadresse?): Adresse? {
        if (bostedsadresse?.vegadresse != null) {
            return Adresse(
                adressenavn = bostedsadresse.vegadresse!!.adressenavn,
                postnummer = bostedsadresse.vegadresse!!.postnummer,
                husnummer = bostedsadresse.vegadresse!!.husnummer,
                husbokstav = bostedsadresse.vegadresse!!.husbokstav,
                bruksenhetnummer = bostedsadresse.vegadresse!!.bruksenhetsnummer,
                bostedskommune = null
            )
        }
        if (bostedsadresse?.matrikkeladresse != null) {
            return Adresse(
                adressenavn = bostedsadresse.matrikkeladresse!!.tilleggsnavn,
                postnummer = bostedsadresse.matrikkeladresse!!.postnummer,
                husnummer = null,
                husbokstav = null,
                bruksenhetnummer = bostedsadresse.matrikkeladresse!!.bruksenhetsnummer,
                bostedskommune = null
            )
        }
        if (bostedsadresse?.ukjentBosted != null) {
            return Adresse(
                adressenavn = null,
                postnummer = null,
                husnummer = null,
                bruksenhetnummer = null,
                husbokstav = null,
                bostedskommune = bostedsadresse?.ukjentBosted!!.bostedskommune
            )
        }
        return null
    }

    private fun mapSivilstandType(sivilstandType: List<PdlSivilstand>): SIVILSTANDTYPE? {
        return if (sivilstandType.isEmpty()) {
            null
        } else {
            return when (sivilstandType.first().type) {
                SIVILSTANDSTYPE.GIFT -> SIVILSTANDTYPE.GIFT
                SIVILSTANDSTYPE.ENKE_ELLER_ENKEMANN -> SIVILSTANDTYPE.ENKE_ELLER_ENKEMANN
                SIVILSTANDSTYPE.SKILT -> SIVILSTANDTYPE.SKILT
                SIVILSTANDSTYPE.SEPARERT -> SIVILSTANDTYPE.SEPARERT
                SIVILSTANDSTYPE.REGISTRERT_PARTNER -> SIVILSTANDTYPE.REGISTRERT_PARTNER
                SIVILSTANDSTYPE.SEPARERT_PARTNER -> SIVILSTANDTYPE.SEPARERT_PARTNER
                SIVILSTANDSTYPE.SKILT_PARTNER -> SIVILSTANDTYPE.SKILT_PARTNER
                SIVILSTANDSTYPE.GJENLEVENDE_PARTNER -> SIVILSTANDTYPE.GJENLEVENDE_PARTNER
                SIVILSTANDSTYPE.UGIFT -> SIVILSTANDTYPE.UGIFT
                SIVILSTANDSTYPE.UOPPGITT -> SIVILSTANDTYPE.UOPPGITT
                SIVILSTANDSTYPE.PARTNER -> SIVILSTANDTYPE.PARTNER
            }
        }
    }

    private fun mapBarn(person: PdlSøkerData) =
        person.familierelasjoner.filter { relasjon ->
            relasjon.relatertPersonsRolle == FAMILIERELASJONSROLLE.BARN
        }.map { relasjon ->
            val barneRespons = hentBarn(relasjon.relatertPersonsIdent)
            val borMedSøker = borMedSøker(
                søkerAdresse = person.bostedsadresse.firstOrNull(),
                barneAdresse = barneRespons.adresse
            )
            Barn(
                ident = relasjon.relatertPersonsIdent,
                adresse = mapAdresser(barneRespons.adresse),
                navn = barneRespons.navn,
                fødselsdato = barneRespons.fødselsdato,
                borMedSøker = borMedSøker
            )
        }.toSet()

    private fun mapStatsborgerskap(statsborgerskap: List<PdlStatsborgerskap>): List<Statborgerskap> {
        return statsborgerskap.map {
            Statborgerskap(
                landkode = it.land
            )
        }
    }
}
