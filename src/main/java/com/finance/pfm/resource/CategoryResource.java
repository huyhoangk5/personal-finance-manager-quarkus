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
    public Response getCategories(@QueryParam("type") Category.TransactionType type, @QueryParam("userId") Long userId) {
        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User ID không được để trống").build();
        }
        
        List<Category> categories;
        if (type != null) {
            categories = categoryService.getCategoriesByUserAndType(userId, type);
        } else {
            categories = categoryService.getCategoriesByUser(userId);
        }
        return Response.ok(categories).build();
    }

    @POST
    public Response createCategory(Category category) {
        if (category.user == null || category.user.userId == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Thông tin người dùng không được để trống").build();
        }
        
        String result = categoryService.createCategory(category);
        if (result.contains("Lỗi")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        
        return Response.ok(category).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateCategory(@PathParam("id") Long id, Category details, @QueryParam("userId") Long userId) {
        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User ID không được để trống").build();
        }
        
        String result = categoryService.updateCategory(id, details, userId);
        if (result.contains("Lỗi")) {
            if (result.contains("không tìm thấy")) {
                return Response.status(Response.Status.NOT_FOUND).entity(result).build();
            } else if (result.contains("không có quyền")) {
                return Response.status(Response.Status.FORBIDDEN).entity(result).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
            }
        }
        
        // Return updated category
        return categoryService.getCategoryById(id)
            .map(cat -> Response.ok(cat).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCategory(@PathParam("id") Long id, @QueryParam("userId") Long userId) {
        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User ID không được để trống").build();
        }
        
        String result = categoryService.deleteCategory(id, userId);
        if (result.contains("Lỗi")) {
            if (result.contains("không tìm thấy")) {
                return Response.status(Response.Status.NOT_FOUND).entity(result).build();
            } else if (result.contains("không có quyền")) {
                return Response.status(Response.Status.FORBIDDEN).entity(result).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
            }
        }
        
        return Response.ok(result).build();
    }
}
