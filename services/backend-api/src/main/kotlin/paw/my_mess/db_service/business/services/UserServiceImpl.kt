package paw.my_mess.db_service.business.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import paw.my_mess.db_service.business.bussines_models.create.BusinessCreateUser
import paw.my_mess.db_service.business.bussines_models.create.BusinessUpdateUser
import paw.my_mess.db_service.business.bussines_models.get.*
import paw.my_mess.db_service.business.error_handling.Response
import paw.my_mess.db_service.business.interfaces.FriendService
import paw.my_mess.db_service.business.interfaces.UserService
import paw.my_mess.db_service.persistence.entities.FriendRequest
import paw.my_mess.db_service.persistence.entities.User
import paw.my_mess.db_service.persistence.entities.UserProfile
import paw.my_mess.db_service.persistence.persistence.interfaces.IFriendRepository
import paw.my_mess.db_service.persistence.persistence.interfaces.IUserProfileRepository
import paw.my_mess.db_service.persistence.persistence.interfaces.IUserRepository
import java.sql.Date
import java.text.SimpleDateFormat
import java.time.LocalDate


@Service
class UserServiceImpl : UserService {

    @Autowired
    private lateinit var _userRepository: IUserRepository<User>

    @Autowired
    private lateinit var _friendService: FriendService

    @Autowired
    private lateinit var _userProfileRepository: IUserProfileRepository<UserProfile>

    @Autowired
    private lateinit var _imageService: ImageServiceImpl

    @Autowired
    private lateinit var _passwordEncoder: PasswordEncoder

    override fun getAllUsers(): Response<List<BusinessUser>?> {
        try {
            val userList = _userRepository.getAll()
            return Response(successful_operation = true, data = userList.map { it.ToBusinessUser() }, code = 200)
        } catch (e: Exception) {
            return Response(successful_operation = false, data = null, code = 400, error = e.toString())
        }
    }

    override fun getUserById(uid: String): Response<BusinessUser?> {
        try {
            val user = _userRepository.get(uid)
            if (user == null) {
                return Response(successful_operation = false, data = user, code = 404, message = "User Not Found")
            }
            return Response(successful_operation = true, data = user.ToBusinessUser(), code = 200)
        } catch (e: NumberFormatException) {
            return Response(successful_operation = false, data = null, code = 400, error = e.toString(), message = "Invalid Id For User")
        } catch (e: Exception) {
            return Response(successful_operation = false, data = null, code = 400, error = e.toString())
        }
    }

    override fun getStrangePeopleByUid(uid: String): Response<List<BusinessUser>?> {
        try {
            val userList = _userRepository.getAll()
            val friendList = _friendService.getFriends(uid, toList = true).data as BusinessFriendshipList
            val blockedList = _friendService.getBlockedFriends(uid, toList = true).data as BusinessBlockedUserList

            val filteredList = userList.filter { !friendList.friendList.contains(it.uid) && !blockedList.blockedUserList.contains(it.uid) && it.uid != uid}.toList()
            return Response(successful_operation = true, data = filteredList.map { it.ToBusinessUser() }, code = 200)
        } catch (e: Exception) {
            return Response(successful_operation = false, data = null, code = 400, error = e.toString())
        }
    }

    override fun createUser(user: BusinessCreateUser): Response<Any?> {
        try {
            if (user.passwordHash == "" || user.username == "") {
                return Response(successful_operation = false, data = user, code = 404, message = "Password or username is null")
            }
            val passwordHash = this._passwordEncoder.encode(user.passwordHash)
            val gender = user.gender.toLowerCase()
            val avatarPath = if (gender == "male") "male.png" else "female.png"
            val uid = _userRepository.add(User(uid = "", userName = user.username, firstname = user.firstName, lastname = user.lastName, passwordHash = passwordHash, email = user.email, avatarPath = avatarPath))
            if (uid == null) {
                return Response(successful_operation = false, data = Unit, code = 400, error = "Can't create user")
            }
            val parsedDate = Date(SimpleDateFormat("yyyy-mm-dd").parse(user.birthdate).time)
            val nowDate = Date.valueOf(LocalDate.now())
            _userProfileRepository.add(UserProfile(uid = uid!!, status = "", birthdate = parsedDate, gender = user.gender, dateRegistered = nowDate, city = user.city, country = user.country))
            return Response(successful_operation = true, data = BusinessId(uid), code = 201)
        } catch (e: org.springframework.dao.DuplicateKeyException) {
            return Response(successful_operation = false, data = Unit, code = 400, error = "User with username ${user.username} is existent")
        } catch (e: java.text.ParseException) {
            return Response(successful_operation = false, data = Unit, code = 400, error = "Invalid Birthdate")
        } catch (e: Exception) {
            return Response(successful_operation = false, data = Unit, code = 400, error = e.toString())
        }
    }

    override fun getAllUserProfiles(): Response<List<BusinessUserProfile>?> {
        try {
            val userList = _userProfileRepository.getAll()
            return Response(successful_operation = true, data = userList.map { it.ToBusinessUserProfile() }, code = 200)
        } catch (e: Exception) {
            return Response(successful_operation = false, data = null, code = 400, error = e.toString())
        }
    }

    override fun getUserProfileById(uid: String): Response<BusinessUserProfile?> {
        try {
            val user = _userProfileRepository.get(uid)
            if (user == null) {
                return Response(successful_operation = false, data = user, code = 404, message = "User Profile Not Found")
            }
            return Response(successful_operation = true, data = user.ToBusinessUserProfile(), code = 200)
        } catch (e: NumberFormatException) {
            return Response(successful_operation = false, data = null, code = 400, error = e.toString(), message = "Invalid Id For User")
        } catch (e: Exception) {
            return Response(successful_operation = false, data = null, code = 400, error = e.toString())
        }
    }

    override fun updateUser(uid: String, user: BusinessUpdateUser): Response<Any?> {
        try {
            if (user.passwordHash == "" || user.username == "") {
                return Response(successful_operation = false, data = user, code = 404, message = "Password or username is null")
            }
            val user_from_db = _userRepository.get(uid)
            val user_profile_from_db = _userProfileRepository.get(uid)
            if (user_from_db == null || user_profile_from_db == null) {
                return Response(successful_operation = false, data = user, code = 404, message = "User Not Found")
            }
            val passwordHash = if (user.passwordHash != null) this._passwordEncoder.encode(user.passwordHash) else user_from_db.passwordHash
            // updatam userul
            val tempUsername = user.username ?: user_from_db.userName
            val tempPasswordhash = passwordHash ?: user_from_db.passwordHash
            val tempEmail = user.email ?: user_from_db.email
            val tempFirstname = user.firstName ?: user_from_db.firstname
            val tempLastname = user.lastName ?: user_from_db.lastname
            var path: String? = null;
            if(user.avatarIcon != null){
                path = _imageService.createFile(uid, user.avatarIcon!!)
                if(path != null && user_from_db.avatarPath != "null")
                    _imageService.deleteFile(user_from_db.avatarPath)
            }

            val tempAvatarpath = path ?: user_from_db.avatarPath
            val userToUpdate = User(uid, tempUsername, tempPasswordhash, tempFirstname, tempLastname, tempEmail, tempAvatarpath)
            _userRepository.update(uid, userToUpdate)

            // updatam user profile-ul
            val tempStatus = user.status ?: user_profile_from_db.status

            val tempBirthdate = if (user.birthdate != null) Date(SimpleDateFormat("yyyy-mm-dd").parse(user.birthdate).time) else user_profile_from_db!!.birthdate
            val tempGender = user.gender ?: user_profile_from_db.gender
            val tempCity = user.city ?: user_profile_from_db.city
            val tempCountry = user.country ?: user_profile_from_db.country
            val nowDate = Date.valueOf(LocalDate.now())
            val userProfileToUpdate = UserProfile(uid, status = tempStatus, birthdate = tempBirthdate, gender = tempGender, city = tempCity, country = tempCountry, dateRegistered = nowDate)
            _userProfileRepository.update(uid, userProfileToUpdate)

            return Response(successful_operation = true, data = Unit, code = 204)
        } catch (e: org.springframework.dao.DuplicateKeyException) {
            return Response(successful_operation = false, data = Unit, code = 400, error = "User with username ${user.username} is existent")
        } catch (e: java.text.ParseException) {
            return Response(successful_operation = false, data = Unit, code = 400, error = "Invalid Birthdate")
        } catch (e: NumberFormatException) {
            return Response(successful_operation = false, data = null, code = 400, error = e.toString(), message = "Invalid Id For User")
        } catch (e: Exception) {
            return Response(successful_operation = false, data = null, code = 400, error = e.toString())
        }
    }

    override fun deleteUserById(uid: String): Response<Any?> {
        try {
            val code = _userProfileRepository.delete(uid)
            if (!code) {
                return Response(successful_operation = false, data = Unit, code = 404, message = "User Not Found")
            }
            _userRepository.delete(uid)
            if (!code) {
                return Response(successful_operation = false, data = Unit, code = 404, message = "User Not Found")
            }
            return Response(successful_operation = true, data = Unit, code = 204)
        } catch (e: Exception) {
            return Response(successful_operation = false, data = null, code = 400, error = e.toString())
        }
    }
}