package com.example.containermanager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BaseStationRepository extends JpaRepository<BaseStation, Integer> {
}

