package com.chithra.resumebuilder.model;

import jakarta.persistence.*;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;
    private String email;
    private String countryCode;
    private String phone;

    @Column(length = 500)
    private String linkedin;

    @Column(length = 500)
    private String github;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] profileImage;

    private String profileImageContentType;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String projects;

    @Column(columnDefinition = "TEXT")
    private String certifications;

    private String experienceLevel;



    private String schoolQualification;
    private String schoolYear;
    private String schoolPercentage;

    private String puQualification;
    private String puYear;
    private String puPercentage;

    private String degreeQualification;
    private String degreeYear;
    private String degreePercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String linkedin) { this.linkedin = linkedin; }

    public String getGithub() { return github; }
    public void setGithub(String github) { this.github = github; }

    public byte[] getProfileImage() { return profileImage; }
    public void setProfileImage(byte[] profileImage) { this.profileImage = profileImage; }

    public String getProfileImageContentType() { return profileImageContentType; }
    public void setProfileImageContentType(String profileImageContentType) {
        this.profileImageContentType = profileImageContentType;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getProjects() { return projects; }
    public void setProjects(String projects) { this.projects = projects; }

    public String getCertifications() { return certifications; }
    public void setCertifications(String certifications) { this.certifications = certifications; }

    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }


    public String getSchoolQualification() { return schoolQualification; }
    public void setSchoolQualification(String schoolQualification) { this.schoolQualification = schoolQualification; }

    public String getSchoolYear() { return schoolYear; }
    public void setSchoolYear(String schoolYear) { this.schoolYear = schoolYear; }

    public String getSchoolPercentage() { return schoolPercentage; }
    public void setSchoolPercentage(String schoolPercentage) { this.schoolPercentage = schoolPercentage; }

    public String getPuQualification() { return puQualification; }
    public void setPuQualification(String puQualification) { this.puQualification = puQualification; }

    public String getPuYear() { return puYear; }
    public void setPuYear(String puYear) { this.puYear = puYear; }

    public String getPuPercentage() { return puPercentage; }
    public void setPuPercentage(String puPercentage) { this.puPercentage = puPercentage; }

    public String getDegreeQualification() { return degreeQualification; }
    public void setDegreeQualification(String degreeQualification) { this.degreeQualification = degreeQualification; }

    public String getDegreeYear() { return degreeYear; }
    public void setDegreeYear(String degreeYear) { this.degreeYear = degreeYear; }

    public String getDegreePercentage() { return degreePercentage; }
    public void setDegreePercentage(String degreePercentage) { this.degreePercentage = degreePercentage; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
