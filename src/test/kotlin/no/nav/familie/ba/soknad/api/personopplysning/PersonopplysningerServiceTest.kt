package no.nav.familie.ba.soknad.api.personopplysning

import io.mockk.every
import io.mockk.mockk
import java.io.File
import no.nav.familie.ba.soknad.api.clients.kodeverk.KodeverkClient
import no.nav.familie.ba.soknad.api.clients.pdl.PdlBrukerClient
import no.nav.familie.ba.soknad.api.clients.pdl.PdlHentPersonResponse
import no.nav.familie.ba.soknad.api.clients.pdl.PdlSystemClient
import no.nav.familie.ba.soknad.api.services.kodeverk.CachedKodeverkService
import no.nav.familie.ba.soknad.api.services.pdl.PersonopplysningerService
import no.nav.familie.ba.soknad.api.services.pdl.mapper.PdlBarnMapper
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PersonopplysningerServiceTest {

    private lateinit var personopplysningerService: PersonopplysningerService
    private lateinit var pdlClient: PdlBrukerClient
    private lateinit var barnePdlClient: PdlSystemClient
    private lateinit var kodeverkClient: KodeverkClient
    private lateinit var kodeverkService: CachedKodeverkService
    private val mapper = objectMapper
    private val gyldigBostedAdresse = Bostedsadresse(
        matrikkeladresse = Matrikkeladresse(3, "E67", "tillegg", "1456", "1223")
    )

    @BeforeEach
    fun setUp() {
        pdlClient = mockk()
        barnePdlClient = mockk()
        kodeverkClient = mockk()
        kodeverkService = mockk()
        personopplysningerService = PersonopplysningerService(pdlClient, barnePdlClient, kodeverkClient)
    }

    @Test
    fun `hentPersonInfo skal kun returnere familierelasjoner av type BARN`() {
        every { pdlClient.hentPerson(any()) } returns pdlMockFor("pdlPersonMedFlereRelasjoner")
        every { barnePdlClient.hentPerson("12345678910") } returns pdlMockFor("pdlPersonBarn")
        every { kodeverkClient.hentPostnummer() } returns kodeverkMockFor("kodeverkPostnummerRespons")
        val person = personopplysningerService.hentPersoninfo("1")

        assertEquals(1, person.barn.size)
        assertEquals("ENGASJERT FYR", person.barn.first().navn)
        assertEquals("23058518298", person.barn.first().ident)
    }

    @Test
    fun `hentPersonInfo skal returnere tom liste hvis det er familierelasjoner, men ingen barn`() {
        every { pdlClient.hentPerson(any()) } returns pdlMockFor("pdlPersonMedRelasjonerIngenBarn")
        every { kodeverkClient.hentPostnummer() } returns kodeverkMockFor("kodeverkPostnummerRespons")
        val person = personopplysningerService.hentPersoninfo("1")
        assertTrue(person.barn.isEmpty())
    }

    @Test
    fun `hentPersonInfo skal returnere ident`() {
        every { pdlClient.hentPerson(any()) } returns pdlMockFor("pdlPersonMedRelasjonerIngenBarn")
        every { kodeverkClient.hentPostnummer() } returns kodeverkMockFor("kodeverkPostnummerRespons")
        val person = personopplysningerService.hentPersoninfo("1")
        assertEquals("23058518298", person.ident)
    }

    @Test
    fun `hentPersonInfo skal returnere liste med statsborgerskap hvis det er flere fra pdl`() {
        every { pdlClient.hentPerson(any()) } returns pdlMockFor("pdlPersonMedFlereStatsborgerskap")
        every { kodeverkClient.hentPostnummer() } returns kodeverkMockFor("kodeverkPostnummerRespons")
        val person = personopplysningerService.hentPersoninfo("1")
        assertEquals(person!!.statsborgerskap.size, 2)
        assertEquals(person.statsborgerskap[0].landkode, "NOR")
        assertEquals(person.statsborgerskap[1].landkode, "SWE")
    }

    @Test
    fun `hentPersonInfo skal returnere tom liste hvis ingen familierelasjoner`() {
        every { pdlClient.hentPerson(any()) } returns pdlMockFor("pdlPersonUtenRelasjoner")
        every { kodeverkClient.hentPostnummer() } returns kodeverkMockFor("kodeverkPostnummerRespons")
        val person = personopplysningerService.hentPersoninfo("1")
        assertTrue(person!!.barn.isEmpty())
    }

    @Test
    fun `hentPersonInfo skal returnere tom liste med barn, der barn er dod`() {
        every { pdlClient.hentPerson(any()) } returns pdlMockFor("pdlBrukerMedDoedBarn")
        every { barnePdlClient.hentPerson("12345678910") } returns pdlMockFor("pdlBarnErDoed")
        every { kodeverkClient.hentPostnummer() } returns kodeverkMockFor("kodeverkPostnummerRespons")
        val person = personopplysningerService.hentPersoninfo("23058518298")
        assertEquals(person.barn.size, 0)
    }
    @Test
    fun `hentPersonInfo skal returnere tom liste med barn, der barn er over atten`() {
        every { pdlClient.hentPerson(any()) } returns pdlMockFor("pdlBarnErOverAtten")
        every { barnePdlClient.hentPerson("12345678910") } returns pdlMockFor("pdlBrukerMedBarnOverAtten")
        every { kodeverkClient.hentPostnummer() } returns kodeverkMockFor("kodeverkPostnummerRespons")
        val person = personopplysningerService.hentPersoninfo("23058518298")
        assertEquals(person.barn.size, 0)
    }

    @Test
    fun `borMedSøker skal returnere false når søkerAdressen er null`() {
        val søkerAdresse = null
        val barneAdresse = null
        val borMedSøker = PdlBarnMapper.borBarnMedSoeker(søkerAdresse, barneAdresse)

        assertFalse(borMedSøker)
    }

    @Test
    fun `borMedSøker skal returnere true når adressene til barn og søker er like`() {
        val borMedSøker = PdlBarnMapper.borBarnMedSoeker(
            soekerAdresse = gyldigBostedAdresse,
            barneAdresse = gyldigBostedAdresse
        )

        assertTrue(borMedSøker)
    }

    @Test
    fun `borMedSøker skal returnere false når søker og barn har ulik adresse, men lik type`() {
        val barneAdresse = gyldigBostedAdresse.copy(
            matrikkeladresse = Matrikkeladresse(
                1,
                "E2",
                "tillegg",
                "1456",
                "1223"
            )
        )
        val borMedSøker = PdlBarnMapper.borBarnMedSoeker(
            soekerAdresse = gyldigBostedAdresse,
            barneAdresse = barneAdresse
        )

        assertFalse(borMedSøker)
    }

    @Test
    fun `borMedSøker skal returnere false hvis søkerAdresse er ukjent`() {
        val ukjentAdresse = Bostedsadresse(ukjentBosted = UkjentBosted("oslo"))
        val borMedSøker = PdlBarnMapper.borBarnMedSoeker(
            soekerAdresse = ukjentAdresse,
            barneAdresse = ukjentAdresse
        )

        assertFalse(borMedSøker)
    }

    @Test
    fun `borMedSøker skal returnere true hvis flere adresser finnes, og minst en matcher`() {
        val søkerAdresse = gyldigBostedAdresse.copy(
            vegadresse = Vegadresse(
                adressenavn = "adresse",
                husbokstav = "A",
                bruksenhetsnummer = "1",
                husnummer = "1",
                kommunenummer = "1",
                matrikkelId = 1,
                postnummer = "0101",
                tilleggsnavn = "tillegg"
            )
        )
        val borMedSøker = PdlBarnMapper.borBarnMedSoeker(
            soekerAdresse = søkerAdresse,
            barneAdresse = gyldigBostedAdresse
        )

        assertTrue(borMedSøker)
    }

    @Test
    fun `hentPerson skal sette flagg om person har adressebeskyttelse`() {
        every { kodeverkClient.hentPostnummer() } returns kodeverkMockFor("kodeverkPostnummerRespons")
        every { pdlClient.hentPerson(any()) } returns pdlMockFor("pdlPersonUtenRelasjonerGradertAdresse")

        val person = personopplysningerService.hentPersoninfo("1")
        assertTrue(person.adressebeskyttelse)
    }

    @Test
    fun `hentPerson skal sette flagg om person har adressebeskyttelse, og returnere null om bruker har adresse`() {
        every { kodeverkClient.hentPostnummer() } returns kodeverkMockFor("kodeverkPostnummerRespons")
        every { pdlClient.hentPerson(any()) } returns pdlMockFor("pdlSoekerHarAdresseOgAdressebeskyttelse")

        val person = personopplysningerService.hentPersoninfo("1")
        assertEquals(person.adresse, null)
        assertEquals(person.adressebeskyttelse, true)
    }

    private fun pdlMockFor(filNavn: String) = mapper.readValue(
        File(getFile("pdl/$filNavn.json")), PdlHentPersonResponse::class.java
    )

    private fun kodeverkMockFor(filNavn: String) = mapper.readValue(
        File(getFile("kodeverk/$filNavn.json")), KodeverkDto::class.java
    )

    private fun getFile(name: String): String {
        return javaClass.classLoader?.getResource(name)?.file ?: error("Testkonfigurasjon feil")
    }

    @Test
    fun `hentPerson returnerer rett adresse fra pdl`() {
        every { pdlClient.hentPerson(any()) } returns pdlMockFor("pdlPersonMedFlereRelasjoner")
        every { barnePdlClient.hentPerson("12345678910") } returns pdlMockFor("pdlPersonBarn")
        every { kodeverkClient.hentPostnummer() } returns kodeverkMockFor("kodeverkPostnummerRespons")

        val person = personopplysningerService.hentPersoninfo("1")
        assertEquals(person.adresse?.adressenavn, "1223")
        assertEquals(person.adresse?.husnummer, "E22")
        assertEquals(person.adresse?.husbokstav, "tillegg")
        assertEquals(person.adresse?.postnummer, "4971")
        assertEquals(person.adresse?.poststed, "SUNDEBRU")
    }

    @Test
    fun `hentPerson sine barn har adressebeskyttelse`() {
        every { pdlClient.hentPerson(any()) } returns pdlMockFor("pdlPersonMedFlereRelasjoner")
        every { barnePdlClient.hentPerson("12345678910") } returns pdlMockFor("pdlBarnHarAdresseBeskyttelse")
        every { kodeverkClient.hentPostnummer() } returns kodeverkMockFor("kodeverkPostnummerRespons")

        val person = personopplysningerService.hentPersoninfo("1")
        assertTrue(person.barn.toList()[0].adressebeskyttelse)
        assertFalse(person.barn.toList()[0].borMedSøker)
    }

    @Test
    fun `hentPerson sine returnerer rett matrikkeladresse`() {
        every { pdlClient.hentPerson(any()) } returns pdlMockFor("pdlMedMatrikkelAdresse")
        every { kodeverkClient.hentPostnummer() } returns kodeverkMockFor("kodeverkPostnummerRespons")
        val person = personopplysningerService.hentPersoninfo("1")
        assertEquals(person.adresse?.adressenavn, "Tilleggsnavn")
        assertEquals(person.adresse?.bruksenhetnummer, "1456")
        assertEquals(person.adresse?.postnummer, "4971")
        assertEquals(person.adresse?.poststed, "SUNDEBRU")
    }
    @Test
    fun `hentPerson sine barn har adressebeskyttelse og barnets navn blir null`() {
        every { pdlClient.hentPerson(any()) } returns pdlMockFor("pdlPersonMedFlereRelasjoner")
        every { barnePdlClient.hentPerson("12345678910") } returns pdlMockFor("pdlBarnHarAdresseBeskyttelse")
        every { kodeverkClient.hentPostnummer() } returns kodeverkMockFor("kodeverkPostnummerRespons")

        val person = personopplysningerService.hentPersoninfo("1")
        assertTrue(person.barn.toList()[0].adressebeskyttelse)
        assertEquals(person.barn.toList()[0].navn, null)
        assertFalse(person.barn.toList()[0].borMedSøker)
    }
}
