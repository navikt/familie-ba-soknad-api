package no.nav.familie.ba.soknad.api.services.pdl.mapper

import no.nav.familie.ba.soknad.api.common.GradertAdresseException
import no.nav.familie.ba.soknad.api.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ba.soknad.api.personopplysning.Adresse
import no.nav.familie.ba.soknad.api.personopplysning.Adressebeskyttelse
import no.nav.familie.ba.soknad.api.personopplysning.Barn
import no.nav.familie.ba.soknad.api.personopplysning.FAMILIERELASJONSROLLE
import no.nav.familie.ba.soknad.api.personopplysning.PdlFamilierelasjon
import no.nav.familie.ba.soknad.api.personopplysning.PdlPersonData
import no.nav.familie.ba.soknad.api.personopplysning.PdlSivilstand
import no.nav.familie.ba.soknad.api.personopplysning.PdlStatsborgerskap
import no.nav.familie.ba.soknad.api.personopplysning.Person
import no.nav.familie.ba.soknad.api.personopplysning.SIVILSTANDSTYPE
import no.nav.familie.ba.soknad.api.personopplysning.SIVILSTANDTYPE
import no.nav.familie.ba.soknad.api.personopplysning.Sivilstand
import no.nav.familie.ba.soknad.api.personopplysning.Statborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse

object PdlMapper {

    fun mapTilPersonInfo(person: PdlPersonData, personIdent: String, barn: Set<Barn>): Person {

        val statsborgerskap: List<Statborgerskap> = mapStatsborgerskap(person.statsborgerskap)
        val sivilstandType = mapSivilstandType(person.sivilstand!!)
        val adresse = mapAdresser(person.bostedsadresse.firstOrNull())

        return Result.runCatching {
            val adresseBeskyttelse = person.adressebeskyttelse
            assertUgradertAdresse(adresseBeskyttelse)

            person.let {
                Person(
                    ident = personIdent,
                    navn = it.navn.first().fulltNavn(),
                    statsborgerskap = statsborgerskap,
                    siviltstatus = Sivilstand(sivilstandType),
                    adresse = adresse,
                    barn = barn
                )
            }
        }.fold(
            onSuccess = { it },
            onFailure = { throw it }
        )
    }

    fun mapFnrBarn(familierelasjoner: List<PdlFamilierelasjon>): List<String> {
        return familierelasjoner.filter { relasjon -> relasjon.relatertPersonsRolle == FAMILIERELASJONSROLLE.BARN }
            .map { it.relatertPersonsIdent }
    }

    private fun mapStatsborgerskap(statsborgerskap: List<PdlStatsborgerskap>): List<Statborgerskap> {
        return statsborgerskap.map {
            Statborgerskap(
                landkode = it.land
            )
        }.distinctBy {
            it.landkode
        }
    }

    fun mapAdresser(bostedsadresse: Bostedsadresse?): Adresse? {
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
            }
        }
    }

    fun assertUgradertAdresse(adresseBeskyttelse: List<Adressebeskyttelse>?) {
        if (adresseBeskyttelse != null) {
            if (adresseBeskyttelse.any { it.gradering != ADRESSEBESKYTTELSEGRADERING.UGRADERT }) {
                throw GradertAdresseException()
            }
        }
    }
}