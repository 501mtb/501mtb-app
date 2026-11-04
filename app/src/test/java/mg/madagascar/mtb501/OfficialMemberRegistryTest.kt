package mg.madagascar.mtb501

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OfficialMemberRegistryTest {

    @Test
    fun officialAssetContains55UniqueSequentialMembers() {
        val asset = File("src/main/assets/501mtb_member_registry.json").readText()
        val members = parseOfficialRegister(asset)

        assertEquals(55, members.size)
        assertEquals(55, members.map { it.memberId }.toSet().size)
        assertEquals((1..55).toList(), members.map { it.memberId.substring(2).toInt() }.sorted())
        assertEquals("M", members.first().gender)
        assertEquals("M-001", members.first().memberId)
    }

    @Test
    fun parserRejectsDuplicateIds() {
        val asset = File("src/main/assets/501mtb_member_registry.json").readText()
        val duplicate = asset.replace("\"memberId\": \"M-002\"", "\"memberId\": \"M-001\"")
        assertThrows(IllegalArgumentException::class.java) {
            parseOfficialRegister(duplicate)
        }
    }

    @Test
    fun parserRejectsWrongRegistryStatus() {
        val asset = File("src/main/assets/501mtb_member_registry.json").readText()
        val unofficial = asset.replaceFirst("\"status\": \"official\"", "\"status\": \"draft\"")
        assertThrows(IllegalArgumentException::class.java) {
            parseOfficialRegister(unofficial)
        }
    }

    @Test
    fun nextIdUsesOneSharedSequenceForBothGenders() {
        assertEquals("M-056", nextOfficialMemberId("M", 55L))
        assertEquals("W-057", nextOfficialMemberId("W", 56L))
    }

    @Test
    fun nextIdRejectsInvalidGenderAndExhaustedSequence() {
        assertThrows(IllegalArgumentException::class.java) {
            nextOfficialMemberId("X", 55L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            nextOfficialMemberId("M", 999L)
        }
    }
}
