package com.kerjalah.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit tests for the Fair-Wage Check (SDG 8), the rule the whole product is
// built around. The interesting cases all sit on or just under the boundary.
//
// Note this only covers the CLIENT-side hint. The binding rule is the
// jobs_pay_per_hour_min_wage CHECK constraint in supabase_migration_01.sql;
// if you change RM8.72 here, change it there too.
//
// Heads-up for anyone running these locally on Windows: `gradlew
// testDebugUnitTest` dies with ClassNotFoundException when the checkout sits
// under a path containing non-ASCII characters (this repo's default location
// has one). The Gradle test worker cannot load the classes, even though the
// classpath is correct - compiling is fine, only the test JVM breaks. Clone to
// an ASCII-only path to run them. CI runs on Linux and is unaffected.
class FairWageTest {

    @Test
    fun `exactly the minimum wage is fair`() {
        // RM8.72 = RM1,700/month under the Minimum Wages Order 2024.
        // The rule is ">= minimum", so the boundary itself must pass.
        assertTrue(FairWage.isFair(8.72))
    }

    @Test
    fun `one sen below the minimum wage is not fair`() {
        assertFalse(FairWage.isFair(8.71))
    }

    @Test
    fun `above the minimum wage is fair`() {
        assertTrue(FairWage.isFair(8.73))
        assertTrue(FairWage.isFair(15.0))
    }

    @Test
    fun `clearly underpaid rates are not fair`() {
        assertFalse(FairWage.isFair(5.0))
        assertFalse(FairWage.isFair(0.0))
    }

    @Test
    fun `negative pay is not fair`() {
        // Can't be typed into the form, but isFair must not treat a parsing
        // accident or a hand-crafted payload as an acceptable wage.
        assertFalse(FairWage.isFair(-1.0))
        assertFalse(FairWage.isFair(-8.72))
    }
}
