package dev.serch.errorplayground.arrow

import arrow.core.*
import arrow.core.computations.either
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.Serializable

class CombineEitherTest {

    @Test
    fun `combine three either happy path should return Right as well`() {
        val maybeOne: Either<String, Int> = 1.right()
        val maybeTwo: Either<Throwable, Int> = 2.right()
        val maybeThree: Either<Boolean, Int> = 3.right()

        val executed = either.eager {
            val one = maybeOne.bind()
            val two = maybeTwo.bind()
            val three = maybeThree.bind()

            one + two + three
        }

        assertTrue(executed.isRight())
        assertEquals(6, executed.orNull())
    }

    @Test
    fun `first either return right should return Left`() {
        val maybeOne: Either<String, Int> = "Failed".left()
        val maybeTwo: Either<Throwable, Int> = 2.right()
        val maybeThree: Either<Boolean, Int> = 3.right()

        val result = either.eager {
            val one = maybeOne.bind()
            val two = maybeTwo.bind()
            val three = maybeThree.bind()

            one + two + three
        }
        assertEquals("Failing first", mapError(result))
    }

    @Test
    fun `second either return right should return Left`() {
        val maybeOne: Either<String, Int> = 1.right()
        val maybeTwo: Either<Throwable, Int> = RuntimeException().left()
        val maybeThree: Either<Boolean, Int> = 3.right()

        val result = either.eager {
            val one = maybeOne.bind()
            val two = maybeTwo.bind()
            val three = maybeThree.bind()

            one + two + three
        }
        assertEquals("Failing second", mapError(result))
    }

    @Test
    fun `third either return right should return Left`() {
        val maybeOne: Either<String, Int> = 1.right()
        val maybeTwo: Either<Throwable, Int> = 2.right()
        val maybeThree: Either<Boolean, Int> = false.left()

        val result = either.eager {
            val one = maybeOne.bind()
            val two = maybeTwo.bind()
            val three = maybeThree.bind()

            one + two + three
        }
        assertEquals("Failing third", mapError(result))
    }

    @Test
    fun `everything fails should return first Left`() {
        val maybeOne: Either<String, Int> = "Failing".left()
        val maybeTwo: Either<Throwable, Int> = RuntimeException().left()
        val maybeThree: Either<Boolean, Int> = false.left()

        val result = either.eager {
            val one = maybeOne.bind()
            val two = maybeTwo.bind()
            val three = maybeThree.bind()

            one + two + three
        }
        assertEquals("Failing first", mapError(result))
    }

    @Test
    fun `recover after first either return left should return Right`() {
        val maybeOne: Either<String, Int> = "1".left()
        val maybeTwo: Either<Throwable, Int> = 2.right()
        val maybeThree: Either<Boolean, Int> = 3.right()

        val executed = either.eager {
            val one = maybeOne.fold(
                ifLeft = { it.toIntOrNull() ?: 0 },
                ifRight = { it }
            )
            val two = maybeTwo.bind()
            val three = maybeThree.bind()

            one + two + three
        }

        assertTrue(executed.isRight())
        assertEquals(6, executed.orNull())
    }

    @Test
    fun `exceptions should be caught and binded to return Left`() {
        val maybeOne: Either<String, Int> = 1.right()
        val maybeTwo: Either<Throwable, Int> = 2.right()
        val maybeThree: Either<Boolean, Int> by lazy { throw RuntimeException() }

        val result = either.eager {
            try {
                val one = maybeOne.bind()
                val two = maybeTwo.bind()
                val three = maybeThree.bind()

                (one + two + three)
            } catch (e: Exception) {
                IllegalStateException().left().bind()
            }
        }
        assertEquals("Dealing with an exception", mapError(result))
    }

    @Test
    fun `mixing different monads should converge to same type to make it works`() {
        val maybeOne: Either<String, Int> = 1.right()
        val maybeTwo: Option<Int> = 2.some()
        val maybeThree: Validated<Boolean, Int> = 3.valid()

        val result = either.eager {
            val one = maybeOne.bind()
            val two = maybeTwo.toEither { RuntimeException() }.bind()
            val three = maybeThree.bind()

            (one + two + three)
        }
        assertEquals(6, result.orNull())
    }

    @Test
    fun `either should call inside a coroutine`() = runTest {
        val maybeOne: Either<String, Int> = 1.right()
        val maybeTwo: Option<Int> = 2.some()
        val maybeThree: Validated<Boolean, Int> = 3.valid()

        suspend fun doTest() = either {
            val one = maybeOne.bind()
            val two = maybeTwo.toEither { RuntimeException() }.bind()
            val three = maybeThree.bind()

            (one + two + three)
        }

        val result = GlobalScope.async {
            doTest()
        }.await()

        assertEquals(6, result.orNull())
    }

    private fun mapError(either: Either<Serializable, Int>): String {
        return either.fold(
            ifLeft = { error ->
                when (error) {
                    is String -> "Failing first"
                    is IllegalStateException -> "Dealing with an exception"
                    is RuntimeException -> "Failing second"
                    is Boolean -> "Failing third"
                    else -> "It wasn't fail"
                }
            },
            ifRight = { "Dealing with an exception" },
        )
    }
}
