package no.nav.familie.ba.soknad.api.domene

import no.nav.familie.kontrakter.ba.søknad.SIVILSTANDTYPE

data class Person(
    val ident: String,
    val navn: String,
    val barn: Set<Barn>,
    val statsborgerskap: List<Statborgerskap>,
    val sivilstand: Sivilstand,
    val adresse: Adresse?,
    val adressebeskyttelse: Boolean
)

data class Barn(
    val ident: String,
    val navn: String?,
    val borMedSøker: Boolean,
    val fødselsdato: String?,
    val adressebeskyttelse: Boolean
)

data class Adresse(
    val adressenavn: String?,
    val postnummer: String?,
    val husnummer: String?,
    val husbokstav: String?,
    val bruksenhetnummer: String?,
    val bostedskommune: String?,
    val poststed: String?
)

data class Statborgerskap(
    val landkode: String
)

data class Sivilstand(
    val type: SIVILSTANDTYPE?
)
