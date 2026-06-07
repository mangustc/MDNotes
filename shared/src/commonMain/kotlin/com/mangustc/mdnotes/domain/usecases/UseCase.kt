package com.mangustc.mdnotes.domain.usecases

//import com.mangustc.mdnotes.domain.usecases.UseCase
//
//data class USECASEInput(
//)
//
//class USECASEUseCase(
//) : UseCase<USECASEInput, Unit> {
//    override suspend fun invoke(input: USECASEInput): Unit {
//        return
//    }
//}

interface UseCase<in Input, out Output> {
    suspend operator fun invoke(input: Input): Output
}