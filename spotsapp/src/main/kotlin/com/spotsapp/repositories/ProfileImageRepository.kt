package com.spotsapp.repositories

import com.spotsapp.entities.ProfileImage
import com.spotsapp.entities.enums.ProfileImageType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProfileImageRepository : JpaRepository<ProfileImage, Long> {

    fun findByUsernameAndImageType(username: String, imageType: ProfileImageType): Optional<ProfileImage>

    fun existsByUsernameAndImageType(username: String, imageType: ProfileImageType): Boolean
}
