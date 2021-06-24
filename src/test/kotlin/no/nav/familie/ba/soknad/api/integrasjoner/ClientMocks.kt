package no.nav.familie.ba.soknad.api.integrasjoner

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.time.LocalDateTime
import no.nav.familie.ba.soknad.api.clients.mottak.MottakClient
import no.nav.familie.ba.soknad.api.clients.pdl.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ba.soknad.api.clients.pdl.Adressebeskyttelse
import no.nav.familie.ba.soknad.api.clients.pdl.FAMILIERELASJONSROLLE
import no.nav.familie.ba.soknad.api.clients.pdl.PdlBrukerClient
import no.nav.familie.ba.soknad.api.clients.pdl.PdlFamilierelasjon
import no.nav.familie.ba.soknad.api.clients.pdl.PdlFolkeregisteridentifikator
import no.nav.familie.ba.soknad.api.clients.pdl.PdlFødselsDato
import no.nav.familie.ba.soknad.api.clients.pdl.PdlHentPersonResponse
import no.nav.familie.ba.soknad.api.clients.pdl.PdlNavn
import no.nav.familie.ba.soknad.api.clients.pdl.PdlPerson
import no.nav.familie.ba.soknad.api.clients.pdl.PdlPersonData
import no.nav.familie.ba.soknad.api.clients.pdl.PdlSivilstand
import no.nav.familie.ba.soknad.api.clients.pdl.PdlStatsborgerskap
import no.nav.familie.ba.soknad.api.clients.pdl.PdlSystemClient
import no.nav.familie.ba.soknad.api.clients.pdl.SIVILSTANDSTYPE
import no.nav.familie.ba.soknad.api.domene.Kvittering
import no.nav.familie.http.sts.StsRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
class ClientMocks {

    @Bean
    @Primary
    @Profile("mock-mottak")
    fun mockMottakClient(): MottakClient {
        val mockMottakClient = mockk<MottakClient>()
        every { mockMottakClient.ping() } just Runs
        every { mockMottakClient.sendSøknad(any()) } returns
            Ressurs.success(Kvittering("søknad mottatt OK", LocalDateTime.now()))
        return mockMottakClient
    }

    @Bean
    @Primary
    @Profile("mock-pdl")
    fun mockPdlClient(): PdlBrukerClient {
        val mockPdlClient = mockk<PdlBrukerClient>()

        every { mockPdlClient.ping() } just Runs
        every { mockPdlClient.hentPerson(any()) } returns PdlHentPersonResponse(
            data = PdlPerson(
                person = PdlPersonData(
                    navn = listOf(
                        PdlNavn(
                            fornavn = "Voksen",
                            etternavn = "Voksnessen"
                        )
                    ),
                    forelderBarnRelasjon = listOf(
                        PdlFamilierelasjon("23456789876", FAMILIERELASJONSROLLE.BARN),
                        PdlFamilierelasjon("12345678987", FAMILIERELASJONSROLLE.BARN),
                        PdlFamilierelasjon("12345678989", FAMILIERELASJONSROLLE.MOR)
                    ),
                    bostedsadresse = listOf(
                        Bostedsadresse(
                            vegadresse = Vegadresse(
                                21,
                                "2",
                                "A",
                                "H0101",
                                "Solveien",
                                "",
                                "",
                                "2304"
                            ),
                            matrikkeladresse = null
                        )
                    ),
                    adressebeskyttelse = emptyList(),
                    statsborgerskap = listOf(
                        PdlStatsborgerskap(
                            land = "NOR"
                        ),
                        PdlStatsborgerskap(
                            land = "AFG"
                        )
                    ),
                    sivilstand = listOf(
                        PdlSivilstand(
                            type = SIVILSTANDSTYPE.GIFT
                        )
                    ),
                    foedsel = listOf(
                        PdlFødselsDato(
                            "2020-02-25"
                        )
                    ),
                    doedsfall = emptyList(),
                    folkeregisteridentifikator = listOf(
                        PdlFolkeregisteridentifikator(
                            identifikasjonsnummer = "23058518298"
                        )
                    )
                )
            ),
            errors = null
        )
        return mockPdlClient
    }

    @Bean
    @Primary
    @Profile("mock-pdl")
    fun mockEkspAuthPdlClient(): PdlSystemClient {
        val mockPdlClient = mockk<PdlSystemClient>()

        every { mockPdlClient.hentPerson("12345678987") } returns PdlHentPersonResponse(
            data = PdlPerson(
                person = PdlPersonData(
                    navn = listOf(PdlNavn("Barn", etternavn = "Barnessen III")),
                    foedsel = listOf(PdlFødselsDato("2010-01-01")),
                    bostedsadresse = listOf(
                        Bostedsadresse(
                            vegadresse = Vegadresse(
                                21,
                                "2",
                                "A",
                                "H0101",
                                "Solveien",
                                "",
                                "",
                                "2304"
                            ),
                            ukjentBosted = null,
                            matrikkeladresse = null
                        )
                    ),
                    adressebeskyttelse = emptyList(),
                    statsborgerskap = emptyList(),
                    sivilstand = emptyList(),
                    doedsfall = emptyList(),
                    folkeregisteridentifikator = listOf(
                        PdlFolkeregisteridentifikator(identifikasjonsnummer = "12345678987")
                    )
                )
            ),
            errors = null
        )
        every { mockPdlClient.hentPerson("23456789876") } returns PdlHentPersonResponse(
            data = PdlPerson(
                person = PdlPersonData(
                    navn = listOf(PdlNavn("Barn", etternavn = "Barnessen II")),
                    foedsel = listOf(PdlFødselsDato("2008-10-01")),
                    bostedsadresse = listOf(
                        Bostedsadresse(
                            vegadresse = Vegadresse(
                                21,
                                "2",
                                "A",
                                "H0101",
                                "Solveien",
                                "",
                                "",
                                "2304"
                            ),
                            ukjentBosted = null,
                            matrikkeladresse = null
                        )
                    ),
                    adressebeskyttelse = listOf(Adressebeskyttelse(ADRESSEBESKYTTELSEGRADERING.FORTROLIG)),
                    statsborgerskap = emptyList(),
                    sivilstand = emptyList(),
                    doedsfall = emptyList(),
                    folkeregisteridentifikator = listOf(
                        PdlFolkeregisteridentifikator(identifikasjonsnummer = "23456789876")
                    )
                )
            ),
            errors = null
        )
        // Catch-all så man kan teste manuell registrerting av barn i dialogen
        every { mockPdlClient.hentPerson(not(or("12345678987", "23456789876"))) } returns PdlHentPersonResponse(
            data = PdlPerson(
                person = PdlPersonData(
                    navn = listOf(PdlNavn("Barn", etternavn = "Barnessen IV")),
                    foedsel = listOf(PdlFødselsDato("2008-10-01")),
                    bostedsadresse = listOf(
                        Bostedsadresse(
                            vegadresse = Vegadresse(
                                21,
                                "2",
                                "A",
                                "H0101",
                                "Solveien",
                                "",
                                "",
                                "2304"
                            ),
                            ukjentBosted = null,
                            matrikkeladresse = null
                        )
                    ),
                    adressebeskyttelse = emptyList(),
                    statsborgerskap = emptyList(),
                    sivilstand = emptyList(),
                    doedsfall = emptyList(),
                    folkeregisteridentifikator = listOf(
                        PdlFolkeregisteridentifikator(identifikasjonsnummer = "31051575728")
                    )
                )
            ),
            errors = null
        )
        return mockPdlClient
    }

    @Bean
    @Primary
    @Profile("mock-sts") fun stsRestClientMock(): StsRestClient {
        val mockStsClient = mockk<StsRestClient>()
        every { mockStsClient.systemOIDCToken } returns ("MOCKED-OIDC-TOKEN")
        return mockStsClient
    }
}
