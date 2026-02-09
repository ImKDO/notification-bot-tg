package boysband.coreservice.client

import boysband.coreservice.dto.ActionDto

interface DbServiceClient {
    //TODO: Мартин не выебет за отсутствие батчей?
    suspend fun getActions(): List<ActionDto>
    suspend fun updateAction(id: Int, action: ActionDto)
    suspend fun deleteAction(id: Int)
}