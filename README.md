# kotlin-error-handling-playground

After trying to find a better way to manage alternative/error flows in Kotlin, we found out that it wasn't as easy as we
expected. This repository tries to find a better way to handle this situation.

## Requirements
The main requirements we had in mind are:

### 1. Write as much as sequential-like code as we can.

We wanted to read the happy path as a sequence. We don't want to deal again with the callback hell,
or whatever solution that generates a pyramid.
Having to deal with all alternative flows in each line is quite annoying.
We'll see this problem in more detail when `Sealed Classes` solution is implemented.

### 2. Method signature should be able to expose all alternative flows.

Alternatives like Unchecked Exceptions (and Kotlin doesn't provide Checked Exceptions), make imposible to trace which
alternative flows could happen. Of course, we could anotate the method with `@Throws`, but everybody knows how would end this story :S. 

### 3. Propagating errors between layers should not imply writing tons of boilerplate

We found that approaches like "sealed classes" with no extra tricks, were really annoying when we have to propagate an error from a layer to the next.
For example, let's imagine a method that perform the sign up. First of all, we'd validate the user, then we'd persist, then we'd send the activation email, etc.

We could end up with a method like this:

```
sealed interface SignUpResult {
    data class CreatedUser(user: User): SignUpResult //when everything went ok
    object DuplicatedUserName: SignUpResult //when the user is invalid
    object CanNotSaveUser: SignUpResult //for whatever unexpected reason
}

...
fun signUp(user: User): SignUpResutl
... 
```

Well, it's easy to imagine that we could rely the validation in a collaborator with a signature like this:

```
sealed interface ValidatedCreation {
    object Success: ValidatedCreation
    object DuplicatedUserName: ValidatedCreation
}

fun validate(user: User): ValidatedCreation
```

As you can see we already have two `DuplicatedUserName`, one for the ValidationLayer and another one for the Use Case layer.
Don't you find the code that converts the validation error into the use case error? We do :S.

### 4. We want to be cool!!!

Well, we'd want to be cool, but not so cool :P. So we don't care at this point about parallelism, reactive programming 
or whatever fancy stuff becoming popular these days. 


## Alternatives
The alternatives we tested were:

1. Using sealed classes to model the problem
2. Exceptions
3. Custom Result (either like)
4. [Arrow's type clases](src/test/kotlin/dev/serch/errorplayground/arrow)
                  
## First conclusions
After some minimal research, we created this matrix:

|                       | Sealed Classes          | Exceptions            | Custom Result         | Arrow |
|---                    |      ---                |  ---                  |       ---             |  ---  |
| Sequential-like code  | Fail                    | Success               | Fail                  | Success using Monad comprehension |
| Completed signature   | Success                 |  Fail with unchecked  | Success               | Success                           |
| Propagate errors      | Tricky                  | Success               | Tricky                | Tricky                            |
| Be cool               | According to docs, yes  | Not at all            | Reinventing the wheel | Indeed                            |
                                    
_Please, do not take this table as the conclusion of an elaborated study. We're just playing with the language and trying write
software the best we can. We're not experts in Kotlin, and we may have different problems than you. Let us know if you find any invalid conclusion of in case you have a better alternative that we missed._

## Methodology

We just tested the scenarios using junit. You'll find a description of each case we wanted to test and, inside the test, the way we'd write the code.
It's simple, and it's testable (by definition) so we think this is the easiest way to do this "research".
