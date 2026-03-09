package com.example.projectexpensetrackeradmin

import androidx.room.*

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(project: Project)

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)

    @Query("SELECT * FROM projects ORDER BY projectName")
    suspend fun getAllProjects(): List<Project>

    @Query("SELECT * FROM projects WHERE projectId = :id")
    suspend fun getProjectById(id: String): Project?

    @Query("DELETE FROM projects")
    suspend fun deleteAll()

    @Query("""
    SELECT * FROM projects
    WHERE (
        projectName LIKE '%' || :text || '%' 
        OR description LIKE '%' || :text || '%'
    )
    AND (:status IS NULL OR projectStatus = :status)
    AND (:owner IS NULL OR projectManager LIKE '%' || :owner || '%')
    AND (:fromDate IS NULL OR startDate >= :fromDate)
    AND (:toDate IS NULL OR endDate <= :toDate)
    ORDER BY projectName
""")
    suspend fun searchProjects(
        text: String,
        status: String?,
        owner: String?,
        fromDate: String?,
        toDate: String?
    ): List<Project>

}