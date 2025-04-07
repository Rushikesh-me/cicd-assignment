package com.tus.product.repos;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tus.product.model.Coupon;

public interface CouponRepo extends JpaRepository<Coupon, Long> {

	Coupon findByCode(String code);

}
