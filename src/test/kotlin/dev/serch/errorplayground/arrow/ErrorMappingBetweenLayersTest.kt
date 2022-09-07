package dev.serch.errorplayground.arrow

import arrow.core.*
import arrow.core.computations.either
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

data class User(val username: String)
sealed interface BaseError

sealed interface DatabaseError: BaseError
sealed interface ValidationError: BaseError
sealed interface SaveUserError: BaseError

data class DuplicatedUserError(val user: User) : DatabaseError, SaveUserError
object DBConnectionError : DatabaseError
data class InvalidUsernameError(val user: User) : ValidationError, SaveUserError
data class MissingMandatoryInfoError(val user: User) : ValidationError
object CanNotSaveUserError : SaveUserError


class ErrorMappingBetweenLayersTest {

    @Test
    fun `no error should return right`() {
        val user = User("arbitrary user")
        val savedUser = saveUserUseCase(
            validationResult = user.valid(),
            persistResult = user.right()
        )
        Assertions.assertEquals(user, savedUser.orNull())
    }

    @Test
    fun `ValidationError that is also SaveUserError should be propagated`() {
        val user = User("arbitrary user")
        val savedUser = saveUserUseCase(
            validationResult = InvalidUsernameError(user).invalid(),
            persistResult = user.right()
        )
        Assertions.assertEquals(InvalidUsernameError(user).left(), savedUser)
    }

    @Test
    fun `PersistError that is also SaveUserError should be propagated`() {
        val user = User("arbitrary user")
        val savedUser = saveUserUseCase(
            validationResult = user.valid(),
            persistResult = DuplicatedUserError(user).left()
        )
        Assertions.assertEquals(
            DuplicatedUserError(user).left(), savedUser
        )
    }

    @Test
    fun `Non mapped error should generate a new different error`() {
        val user = User("arbitrary user")
        val savedUser = saveUserUseCase(
            validationResult = user.valid(),
            persistResult = DBConnectionError.left()
        )
        Assertions.assertEquals(
            CanNotSaveUserError.left(), savedUser
        )
    }

    private fun saveUserUseCase(
        validationResult: Validated<ValidationError, User>,
        persistResult: Either<DatabaseError, User>
    ): Either<SaveUserError, User> {
        fun validatedUser(): Validated<ValidationError, User> = validationResult
        fun persistUser(user: User): Either<DatabaseError, User> = persistResult
        fun createdUser(user: User): Either<SaveUserError, User> = user.right()

        val happyPath = either.eager {
            val validated = validatedUser().bind()
            val saved = persistUser(validated).bind()
            createdUser(saved).bind()
        }
        return happyPath.mapLeft { mapError(it) }
    }

    private fun mapError(error: BaseError): SaveUserError =
        when (error) {
            is SaveUserError -> error
            is DatabaseError -> mapDatabaseError(error)
            is ValidationError -> mapValidationError(error)
        }


    private fun mapDatabaseError(databaseError: DatabaseError): SaveUserError =
        when (databaseError) {
            is SaveUserError -> databaseError
            is DBConnectionError -> CanNotSaveUserError
        }

    private fun mapValidationError(validationError: ValidationError): SaveUserError =
        when (validationError) {
            is SaveUserError -> validationError
            is MissingMandatoryInfoError -> CanNotSaveUserError
        }


}
