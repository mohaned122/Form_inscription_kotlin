package tn.tp.atelierandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupModelTest {

    @Test
    fun testGroupCreationAndProperties() {
        val group = Group(id = 1L, name = "G4", capacity = 15)
        assertEquals(1L, group.id)
        assertEquals("G4", group.name)
        assertEquals(15, group.capacity)
    }

    @Test
    fun testStudentModelHelpers() {
        val student = Student(
            id = 10L,
            nom = "Dupont",
            prenom = "Jean",
            age = 22,
            groupe = "G1",
            support = "Émulateur",
            isWaiting = false
        )

        assertEquals("Dupont Jean", student.nomComplet)
        assertEquals("22 ans · Émulateur", student.infos)
        assertFalse(student.isWaiting)
    }

    @Test
    fun testWaitingStudentModel() {
        val waitingStudent = Student(
            id = 11L,
            nom = "Martin",
            prenom = "Claire",
            age = 20,
            groupe = "G1",
            support = "Téléphone réel",
            isWaiting = true
        )

        assertTrue(waitingStudent.isWaiting)
        assertEquals("Martin Claire", waitingStudent.nomComplet)
    }
}
