package com.ljq.petagent.controller;

import com.ljq.petagent.entity.AppUser;
import com.ljq.petagent.entity.Cart;
import com.ljq.petagent.entity.CartItem;
import com.ljq.petagent.entity.Dog;
import com.ljq.petagent.entity.Order;
import com.ljq.petagent.entity.OrderItem;
import com.ljq.petagent.entity.Product;
import com.ljq.petagent.repository.CartItemRepository;
import com.ljq.petagent.repository.CartRepository;
import com.ljq.petagent.repository.DogRepository;
import com.ljq.petagent.repository.OrderItemRepository;
import com.ljq.petagent.repository.OrderRepository;
import com.ljq.petagent.repository.ProductRepository;
import com.ljq.petagent.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CartOrderController {

    private final CurrentUserService currentUserService;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final DogRepository dogRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public CartOrderController(
        CurrentUserService currentUserService,
        CartRepository cartRepository,
        CartItemRepository cartItemRepository,
        ProductRepository productRepository,
        DogRepository dogRepository,
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository
    ) {
        this.currentUserService = currentUserService;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.dogRepository = dogRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping("/cart/add/{productId}")
    public String addProduct(@PathVariable Long productId, Authentication authentication) {
        AppUser user = currentUserService.require(authentication);
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Cart cart = findOrCreateCart(user);
        CartItem item = cartItemRepository.findByCartIdAndProduct(cart.getId(), product).orElse(null);
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(1);
        } else {
            item.setQuantity(item.getQuantity() + 1);
        }
        cartItemRepository.save(item);
        return "redirect:/cart/";
    }

    @GetMapping("/cart/add-dog/{dogId}")
    public String addDog(@PathVariable Long dogId, Authentication authentication) {
        AppUser user = currentUserService.require(authentication);
        Dog dog = dogRepository.findByIdAndPublishedTrue(dogId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Cart cart = findOrCreateCart(user);
        CartItem item = cartItemRepository.findByCartIdAndDog(cart.getId(), dog).orElse(null);
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setDog(dog);
            item.setQuantity(1);
        } else {
            item.setQuantity(item.getQuantity() + 1);
        }
        cartItemRepository.save(item);
        return "redirect:/cart/";
    }

    @GetMapping("/cart/remove/{itemId}")
    public String remove(@PathVariable Long itemId, Authentication authentication) {
        AppUser user = currentUserService.require(authentication);
        CartItem item = requireOwnedItem(itemId, user);
        cartItemRepository.delete(item);
        return "redirect:/cart/";
    }

    @GetMapping("/cart/quantity/{itemId}/{action}")
    public String quantity(
        @PathVariable Long itemId,
        @PathVariable String action,
        Authentication authentication
    ) {
        AppUser user = currentUserService.require(authentication);
        CartItem item = requireOwnedItem(itemId, user);
        if ("inc".equals(action)) {
            item.setQuantity(item.getQuantity() + 1);
            cartItemRepository.save(item);
        } else if ("dec".equals(action)) {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                cartItemRepository.save(item);
            } else {
                cartItemRepository.delete(item);
            }
        }
        return "redirect:/cart/";
    }

    @GetMapping("/cart")
    public String cart(Authentication authentication, Model model) {
        AppUser user = currentUserService.require(authentication);
        Cart cart = cartRepository.findFirstByUserIdOrderByIdAsc(user.getId()).orElse(null);
        List<CartItem> items = cart == null
            ? new ArrayList<CartItem>()
            : cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            total = total.add(item.getLineTotal());
        }
        model.addAttribute("cart", cart);
        model.addAttribute("cartItems", items);
        model.addAttribute("total", total);
        model.addAttribute("activeNav", "cart");
        return "pets/cart";
    }

    @GetMapping("/checkout")
    public String checkoutForm(Authentication authentication, Model model) {
        AppUser user = currentUserService.require(authentication);
        Cart cart = cartRepository.findFirstByUserIdOrderByIdAsc(user.getId()).orElse(null);
        if (cart == null) {
            return "redirect:/cart/";
        }
        List<CartItem> items = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        if (items.isEmpty()) {
            return "redirect:/cart/";
        }
        model.addAttribute("cartItems", items);
        model.addAttribute("total", calculateTotal(items));
        return "pets/checkout";
    }

    @PostMapping("/checkout")
    @Transactional
    public String checkout(
        @RequestParam String phone,
        @RequestParam String address,
        Authentication authentication,
        Model model
    ) {
        AppUser user = currentUserService.require(authentication);
        Cart cart = cartRepository.findFirstByUserIdOrderByIdAsc(user.getId()).orElse(null);
        if (cart == null) {
            return "redirect:/cart/";
        }
        List<CartItem> items = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId());
        if (items.isEmpty()) {
            return "redirect:/cart/";
        }
        if (phone == null || phone.trim().isEmpty() || address == null || address.trim().isEmpty()) {
            model.addAttribute("cartItems", items);
            model.addAttribute("total", calculateTotal(items));
            model.addAttribute("checkoutError", "请填写联系电话和收货地址");
            return "pets/checkout";
        }
        Order order = new Order();
        order.setUser(user);
        order.setPhone(phone);
        order.setAddress(address);
        order.setTotal(calculateTotal(items));
        order.setStatus("pending");
        orderRepository.save(order);
        for (CartItem item : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(item.getProduct());
            orderItem.setDog(item.getDog());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(item.getUnitPrice());
            orderItemRepository.save(orderItem);
        }
        cartItemRepository.deleteByCartId(cart.getId());
        return "redirect:/orders/" + order.getId() + "/";
    }

    @GetMapping("/orders")
    public String orders(Authentication authentication, Model model) {
        AppUser user = currentUserService.require(authentication);
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        Map<Long, Integer> orderItemCounts = new HashMap<Long, Integer>();
        for (Order order : orders) {
            orderItemCounts.put(order.getId(), orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()).size());
        }
        model.addAttribute("orders", orders);
        model.addAttribute("orderItemCounts", orderItemCounts);
        return "pets/order_list";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Authentication authentication, Model model) {
        AppUser user = currentUserService.require(authentication);
        Order order = requireOwnedOrder(id, user);
        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItemRepository.findByOrderIdOrderByIdAsc(id));
        return "pets/order_detail";
    }

    @GetMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication authentication) {
        AppUser user = currentUserService.require(authentication);
        Order order = requireOwnedOrder(id, user);
        if ("pending".equals(order.getStatus())) {
            order.setStatus("cancelled");
            orderRepository.save(order);
        }
        return "redirect:/orders/" + id + "/";
    }

    @GetMapping("/orders/{id}/pay")
    public String pay(@PathVariable Long id, Authentication authentication) {
        AppUser user = currentUserService.require(authentication);
        Order order = requireOwnedOrder(id, user);
        if ("pending".equals(order.getStatus())) {
            order.setStatus("paid");
            orderRepository.save(order);
        }
        return "redirect:/orders/" + id + "/";
    }

    private Cart findOrCreateCart(AppUser user) {
        Cart cart = cartRepository.findFirstByUserIdOrderByIdAsc(user.getId()).orElse(null);
        if (cart != null) {
            return cart;
        }
        cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    private CartItem requireOwnedItem(Long id, AppUser user) {
        CartItem item = cartItemRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return item;
    }

    private Order requireOwnedOrder(Long id, AppUser user) {
        return orderRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private BigDecimal calculateTotal(List<CartItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            total = total.add(item.getLineTotal());
        }
        return total;
    }
}
