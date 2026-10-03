package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.ReviewRequest;
import com.shopsphere.ecommerce.dto.ReviewResponse;
import com.shopsphere.ecommerce.entity.Product;
import com.shopsphere.ecommerce.entity.Review;
import com.shopsphere.ecommerce.entity.Role;
import com.shopsphere.ecommerce.entity.User;
import com.shopsphere.ecommerce.exception.BadRequestException;
import com.shopsphere.ecommerce.exception.ConflictException;
import com.shopsphere.ecommerce.exception.ForbiddenException;
import com.shopsphere.ecommerce.exception.ProductNotFoundException;
import com.shopsphere.ecommerce.exception.ResourceNotFoundException;
import com.shopsphere.ecommerce.repository.ProductRepository;
import com.shopsphere.ecommerce.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    /** The reviewer is ALWAYS the logged-in user - never taken from the body. */
    @Transactional
    public ReviewResponse create(User user, ReviewRequest request) {

        if (request.getProductId() == null) {
            throw new BadRequestException("productId is required");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        if (reviewRepository
                .findByUserIdAndProductId(user.getId(), product.getId())
                .isPresent()) {
            throw new ConflictException("You have already reviewed this product");
        }

        Review review = new Review(
                request.getRating(), request.getComment(), user, product);

        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listForProduct(Long productId) {

        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(
                    "Product not found with id: " + productId);
        }

        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public ReviewResponse update(User user, Long id, ReviewRequest request) {
        Review review = findAllowed(user, id);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public void delete(User user, Long id) {
        reviewRepository.delete(findAllowed(user, id));
    }

    // owner or admin only
    private Review findAllowed(User user, Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        boolean owner = review.getUser().getId().equals(user.getId());
        if (!owner && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You can only change your own review");
        }
        return review;
    }
}
