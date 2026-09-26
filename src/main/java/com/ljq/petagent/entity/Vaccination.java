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
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pets_vaccination")
public class Vaccination implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private PetProfile pet;

    @Column(name = "vaccine_type", nullable = false, length = 20)
    private String vaccineType;

    @Column(name = "vaccine_name", nullable = false, length = 100)
    private String vaccineName;

    @Column(name = "dose_number", nullable = false)
    private Integer doseNumber;

    @Column(name = "administered_date", nullable = false)
    private LocalDate administeredDate;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(nullable = false, length = 100)
    private String clinic;

    @Column(nullable = false, length = 50)
    private String veterinarian;

    @Column(nullable = false, columnDefinition = "longtext")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PetProfile getPet() {
        return pet;
    }

    public void setPet(PetProfile pet) {
        this.pet = pet;
    }

    public String getVaccineType() {
        return vaccineType;
    }

    public void setVaccineType(String vaccineType) {
        this.vaccineType = vaccineType;
    }

    public String getVaccineName() {
        return vaccineName;
    }

    public void setVaccineName(String vaccineName) {
        this.vaccineName = vaccineName;
    }

    public Integer getDoseNumber() {
        return doseNumber;
    }

    public void setDoseNumber(Integer doseNumber) {
        this.doseNumber = doseNumber;
    }

    public LocalDate getAdministeredDate() {
        return administeredDate;
    }

    public void setAdministeredDate(LocalDate administeredDate) {
        this.administeredDate = administeredDate;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(LocalDate nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public String getClinic() {
        return clinic;
    }

    public void setClinic(String clinic) {
        this.clinic = clinic;
    }

    public String getVeterinarian() {
        return veterinarian;
    }

    public void setVeterinarian(String veterinarian) {
        this.veterinarian = veterinarian;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getVaccineTypeDisplay() {
        switch (vaccineType == null ? "" : vaccineType) {
            case "rabies":
                return "狂犬疫苗";
            case "dhpp":
                return "犬瘟/细小/肝炎/副流感四联";
            case "dhpp_plus":
                return "犬八联疫苗";
            case "fvrpc":
                return "猫三联(猫瘟/猫鼻支/猫杯状)";
            case "felv":
                return "猫白血病疫苗";
            default:
                return "其他疫苗";
        }
    }
}
