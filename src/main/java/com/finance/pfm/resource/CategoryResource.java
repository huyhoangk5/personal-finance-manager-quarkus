package com.finance.pfm.resource;

import com.finance.pfm.entity.Category;
import com.finance.pfm.repository.CategoryRepository;
import com.finance.pfm.service.CategoryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
    public Response getCategories(@QueryParam("type") Category.TransactionType type) {
        List<Category> categories;
        if (type != null) {
            categories = categoryRepository.findByType(type);
        } else {
            categories = categoryRepository.listAll();
        }
        return Response.ok(categories).build();
    }

    @POST
    @jakarta.transaction.Transactional
    public Category createCategory(Category category) {
        categoryRepository.persist(category);
        return category;
    }

    @PUT
    @Path("/{id}")
    @jakarta.transaction.Transactional
    public Response updateCategory(@PathParam("id") Long id, Category details) {
        Category cat = categoryRepository.findById(id);
        if (cat != null) {
            cat.categoryName = details.categoryName;
            cat.type = details.type;
            return Response.ok(cat).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCategory(@PathParam("id") Long id) {
        categoryService.deleteCategory(id);
        return Response.ok().build();
    }
}
