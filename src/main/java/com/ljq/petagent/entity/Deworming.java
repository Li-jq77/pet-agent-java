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
@Table(name = "pets_deworming")
public class Deworming implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private PetProfile pet;

    @Column(name = "deworming_type", nullable = false, length = 10)
    private String dewormingType;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "administered_date", nullable = false)
    private LocalDate administeredDate;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

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

    public String getDewormingType() {
        return dewormingType;
    }

    public void setDewormingType(String dewormingType) {
        this.dewormingType = dewormingType;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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

    public String getDewormingTypeDisplay() {
        switch (dewormingType == null ? "" : dewormingType) {
            case "internal":
                return "体内驱虫";
            case "external":
                return "体外驱虫";
            case "both":
                return "体内外同驱";
            default:
                return "";
        }
    }
}
