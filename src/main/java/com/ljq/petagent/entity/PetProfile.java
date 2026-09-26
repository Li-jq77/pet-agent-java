package com.ljq.petagent.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Entity
@Table(name = "pets_petprofile")
public class PetProfile implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String species;

    @Column(nullable = false, length = 100)
    private String breed;

    @Column(nullable = false, length = 10)
    private String gender;

    private LocalDate birthday;

    @Column(name = "adopted_date")
    private LocalDate adoptedDate;

    @Column(name = "avatar_url", nullable = false, length = 500)
    private String avatarUrl;

    @Column(nullable = false, length = 100)
    private String color;

    @Column(name = "microchip_id", nullable = false, length = 50)
    private String microchipId;

    @Column(nullable = false, columnDefinition = "longtext")
    private String notes;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private BigDecimal latestWeight;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public LocalDate getAdoptedDate() {
        return adoptedDate;
    }

    public void setAdoptedDate(LocalDate adoptedDate) {
        this.adoptedDate = adoptedDate;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getMicrochipId() {
        return microchipId;
    }

    public void setMicrochipId(String microchipId) {
        this.microchipId = microchipId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BigDecimal getLatestWeight() {
        return latestWeight;
    }

    public void setLatestWeight(BigDecimal latestWeight) {
        this.latestWeight = latestWeight;
    }

    public String getAge() {
        if (birthday == null) {
            return null;
        }
        Period period = Period.between(birthday, LocalDate.now());
        int years = period.getYears();
        int months = period.getMonths();
        if (years > 0) {
            return years + "岁" + months + "个月";
        }
        return months + "个月";
    }

    public String getSpeciesDisplay() {
        switch (species == null ? "" : species) {
            case "dog":
                return "🐶 狗狗";
            case "cat":
                return "🐱 猫咪";
            case "rabbit":
                return "🐰 兔子";
            case "bird":
                return "🐦 小鸟";
            case "fish":
                return "🐟 鱼类";
            case "hamster":
                return "🐹 仓鼠";
            case "turtle":
                return "🐢 乌龟";
            default:
                return "其他";
        }
    }

    public String getGenderDisplay() {
        return "female".equals(gender) ? "♀️ 女生" : "♂️ 男生";
    }
}
