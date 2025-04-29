package com.veststore.veststoreback.service;

import com.veststore.veststoreback.dto.UserDto;
import com.veststore.veststoreback.exception.ResourceNotFoundException;
import com.veststore.veststoreback.model.Cart;
import com.veststore.veststoreback.model.Role;
import com.veststore.veststoreback.model.User;
import com.veststore.veststoreback.repository.CartRepository;
import com.veststore.veststoreback.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, CartRepository cartRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User createUser(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User();
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setAddress(userDto.getAddress());
        user.setPhone(userDto.getPhone());
        user.setRoles(Collections.singleton(Role.ROLE_USER));
        user = userRepository.save(user);

        // Create an empty cart for the new user
        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.save(cart);

        return user;
    }

    @Transactional
    public User updateUser(Long id, UserDto userDto) {
        User user = getUserById(id);

        if (userDto.getName() != null) {
            user.setName(userDto.getName());
        }

        if (userDto.getEmail() != null && !userDto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(userDto.getEmail())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(userDto.getEmail());
        }

        if (userDto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        if (userDto.getAddress() != null) {
            user.setAddress(userDto.getAddress());
        }

        if (userDto.getPhone() != null) {
            user.setPhone(userDto.getPhone());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public User addAdminRole(Long id) {
        User user = getUserById(id);
        user.getRoles().add(Role.ROLE_ADMIN);
        return userRepository.save(user);
    }

    @Transactional
    public User removeAdminRole(Long id) {
        User user = getUserById(id);
        user.getRoles().remove(Role.ROLE_ADMIN);
        return userRepository.save(user);
    }
}

