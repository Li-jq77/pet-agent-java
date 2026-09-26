package com.ljq.petagent.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "pets_cartitem")
public class CartItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dog_id")
    private Dog dog;

    @Column(nullable = false)
    private Integer quantity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Dog getDog() {
        return dog;
    }

    public void setDog(Dog dog) {
        this.dog = dog;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDisplayName() {
        if (dog != null) {
            return dog.getName();
        }
        return product == null ? "未知商品" : product.getName();
    }

    public BigDecimal getUnitPrice() {
        if (dog != null) {
            return dog.getPrice();
        }
        return product == null ? BigDecimal.ZERO : product.getPrice();
    }

    public BigDecimal getLineTotal() {
        return getUnitPrice().multiply(BigDecimal.valueOf(quantity == null ? 0 : quantity));
    }

    public String getImageUrl() {
        if (dog != null) {
            return dog.getImageUrl();
        }
        return product == null ? "" : product.getImageUrl();
    }

    public String getCategoryIcon() {
        if (dog != null) {
            return dog.getCategory().getIcon();
        }
        return product == null ? "🐾" : product.getCategory().getIcon();
    }

    public String getItemType() {
        return dog == null ? "商品" : "宠物";
    }
}
