package com.finance.pfm.resource;

import com.finance.pfm.entity.Category;
import com.finance.pfm.service.CategoryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/categories")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Categories", description = "Quản lý danh mục thu chi")
public class CategoryResource {

    @Inject
    CategoryService categoryService;

    @GET
    @Operation(summary = "Lấy danh sách danh mục của user đang đăng nhập")
    public Response getCategories(@QueryParam("type") Category.TransactionType type, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());

        List<Category> categories;
        if (type != null) {
            categories = categoryService.getCategoriesByUserAndType(userId, type);
        } else {
            categories = categoryService.getCategoriesByUser(userId);
        }
        return Response.ok(categories).build();
    }

    @POST
    @Operation(summary = "Tạo danh mục mới")
    public Response createCategory(Category category, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        // Gán userId từ token
        if (category.user == null) {
            category.user = new com.finance.pfm.entity.User();
        }
        category.user.userId = userId;

        String result = categoryService.createCategory(category);
        if (result.contains("Lỗi")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        return Response.ok(category).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Cập nhật danh mục")
    public Response updateCategory(@PathParam("id") Long id, Category details, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());

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

        return categoryService.getCategoryById(id)
                .map(cat -> Response.ok(cat).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Xóa danh mục")
    public Response deleteCategory(@PathParam("id") Long id, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());

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
