package no.nav.familie.ba.soknad.api.services.pdl.mapper

import no.nav.familie.ba.soknad.api.clients.pdl.PdlHentPersonResponse
import no.nav.familie.ba.soknad.api.domene.Barn
import no.nav.familie.ba.soknad.api.services.pdl.mapper.PdlMapper.harPersonAdresseBeskyttelse
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse

object PdlBarnMapper {

    fun borBarnMedSoeker(soekerAdresse: Bostedsadresse?, barneAdresse: Bostedsadresse?): Boolean {
        fun adresseListe(bostedsadresse: Bostedsadresse): List<Any?> {
            return listOfNotNull(bostedsadresse.matrikkeladresse, bostedsadresse.vegadresse)
        }

        return if (soekerAdresse == null || barneAdresse == null) false

        else {
            val soekerAdresser = adresseListe(soekerAdresse)
            val barneAdresser = adresseListe(barneAdresse)
            soekerAdresser.any { barneAdresser.contains(it) }
        }
    }

    fun mapBarn(
        barnRespons: PdlHentPersonResponse,
        soekerAdresse: Bostedsadresse?,
    ): Barn {
        return Result.runCatching {
            val barnHarAdresseBeskyttelse = harPersonAdresseBeskyttelse(barnRespons.data.person?.adressebeskyttelse)
            Barn(
                ident = barnRespons.data.person?.folkeregisteridentifikator?.first()?.identifikasjonsnummer!!,
                navn = if (barnHarAdresseBeskyttelse) { null } else { barnRespons.data.person.navn.firstOrNull()!!.fulltNavn() },
                fødselsdato = barnRespons.data.person.foedsel.firstOrNull()?.foedselsdato,
                borMedSøker = when (barnHarAdresseBeskyttelse) {
                    true -> false
                    false -> borBarnMedSoeker(
                        soekerAdresse = soekerAdresse,
                        barneAdresse = barnRespons.data.person.bostedsadresse.firstOrNull()
                    )
                },
                adressebeskyttelse = barnHarAdresseBeskyttelse
            )
        }.fold(
            onSuccess = { it },
            onFailure = { throw it }
        )
    }
}
