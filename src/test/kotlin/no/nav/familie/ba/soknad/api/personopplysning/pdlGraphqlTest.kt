package no.nav.familie.ba.soknad.api.personopplysning

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class PdlGraphqlTest {

    private val mapper = ObjectMapper().registerKotlinModule()

    @Test
    fun testDeserialization() {
        val resp = mapper.readValue(File(getFile("pdl/pdlPersonUtenRelasjoner.json")), PdlHentPersonResponse::class.java)

        assertEquals("ENGASJERT", resp.data.person!!.navn.first().fornavn)
        assertEquals("FYR", resp.data.person!!.navn.first().fornavn)
        assertEquals(emptyList<PdlFamilierelasjon>(), resp.data.person!!.familierelasjoner)
    }

    @Test
    fun testDeserializationOfResponseWithErrors() {
        val resp = mapper.readValue(File(getFile("pdl/pdlPersonIkkeFunnetResponse.json")), PdlHentPersonResponse::class.java)
        assertTrue(resp.harFeil())
        assertTrue(resp.errorMessages().contains("Fant ikke person"))
        assertTrue(resp.errorMessages().contains("Ikke tilgang"))
    }

    @Test  // Stjålet nesten ordrett fra familie-integrasjon: PdlGraphqlTest.
    fun testFulltNavn() {
        assertEquals(
                "For Mellom Etter",
                PdlNavn(fornavn = "For", mellomnavn = "Mellom", etternavn = "Etter").fulltNavn())
        assertEquals(
                "For Etter",
                PdlNavn(fornavn = "For", etternavn = "Etter").fulltNavn())
    }

    private fun getFile(name: String): String {
        return javaClass.classLoader?.getResource(name)?.file ?: error("Testkonfigurasjon feil")
    }
}