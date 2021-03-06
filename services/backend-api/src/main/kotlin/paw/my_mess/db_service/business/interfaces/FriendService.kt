package paw.my_mess.db_service.business.interfaces

import paw.my_mess.db_service.business.bussines_models.get.BusinessBlockedUser
import paw.my_mess.db_service.business.bussines_models.get.BusinessFriendRequest
import paw.my_mess.db_service.business.bussines_models.get.BusinessFriendship
import paw.my_mess.db_service.business.bussines_models.get.BusinessSentFriendRequestList
import paw.my_mess.db_service.business.error_handling.Response

interface FriendService {
    fun getAllFriendships(): Response<List<BusinessFriendship>>
    fun getAllFriendRequests(): Response<List<BusinessFriendRequest>>
    fun getAllBlockedFriends(): Response<List<BusinessBlockedUser>>
    fun sendFriendRequest(senderId: String, targetId: String): Response<Any?>
    fun refuseFriendRequest(senderId: String, targetId: String): Response<Any?>
    fun acceptFriendRequest(senderId: String, targetId: String): Response<Any?>
    fun blockFriend(userId: String, targetId: String): Response<Any?>
    fun unblockFriend(userId: String, targetId: String): Response<Any?>
    fun getFriends(userId: String, toList: Boolean=false): Response<Any?>
    fun getFriendship(userId: String): Response<BusinessFriendship>
    fun getBlockedFriends(userId: String, toList: Boolean=false): Response<Any?>
    fun removeFriend(userId: String, targetId: String): Response<Any?>
    fun getFriendRequests(userId: String): Response<List<BusinessFriendRequest>>
    fun getSentFriendRequests(userId: String): Response<BusinessSentFriendRequestList>
}