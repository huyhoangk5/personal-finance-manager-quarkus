package com.finance.pfm.resource;

import com.finance.pfm.entity.Category;
import com.finance.pfm.repository.CategoryRepository;
import com.finance.pfm.service.CategoryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

@Path("/api/categories")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryResource {

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    CategoryService categoryService;

    @GET
    public Response getCategories(@QueryParam("type") Category.TransactionType type, @QueryParam("userId") Long userId) {
        List<Category> categories;
        if (userId != null) {
            if (type != null) {
                categories = categoryRepository.findByTypeAndUser_UserId(type, userId);
            } else {
                categories = categoryRepository.findByUser_UserId(userId);
            }
        } else {
            // Fallback for system categories or legacy if userId is missing, 
            // but in production we should probably require userId.
            if (type != null) {
                categories = categoryRepository.findByType(type);
            } else {
                categories = categoryRepository.listAll();
            }
        }
        return Response.ok(categories).build();
    }

    @POST
    @jakarta.transaction.Transactional
    public Category createCategory(Category category) {
        if (category.user == null || category.user.userId == null) {
            throw new WebApplicationException("User context is required", Response.Status.BAD_REQUEST);
        }
        categoryRepository.persist(category);
        return category;
    }

    @PUT
    @Path("/{id}")
    @jakarta.transaction.Transactional
    public Response updateCategory(@PathParam("id") Long id, Category details, @QueryParam("userId") Long userId) {
        Category cat = categoryRepository.findById(id);
        if (cat != null) {
            if (userId == null || (cat.user != null && !cat.user.userId.equals(userId))) {
                return Response.status(Response.Status.FORBIDDEN).entity("Access denied").build();
            }
            cat.categoryName = details.categoryName;
            cat.type = details.type;
            return Response.ok(cat).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCategory(@PathParam("id") Long id, @QueryParam("userId") Long userId) {
        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("userId is required").build();
        }
        categoryService.deleteCategory(id, userId);
        return Response.ok().build();
    }
}
