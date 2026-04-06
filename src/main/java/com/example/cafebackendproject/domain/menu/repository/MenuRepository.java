package com.example.cafebackendproject.domain.menu.repository;

import com.example.cafebackendproject.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    boolean existsByNameAndDeletedFalse(String name);

    List<Menu> findAllByIsAvailableTrueAndDeletedFalse();

    List<Menu> findAllByDeletedFalse();
}
