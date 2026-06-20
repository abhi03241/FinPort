package com.expensetracker.expensetracker.controllers;

import com.expensetracker.expensetracker.models.Category;
import com.expensetracker.expensetracker.models.Transaction;
import com.expensetracker.expensetracker.services.CategoryService;
import com.expensetracker.expensetracker.services.TransactionRepository;
import com.expensetracker.expensetracker.services.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/transactions")
public class TransactionsController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CategoryService categoryService;

    // Method to list all transactions
    @GetMapping
    public String listTransactions(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(required = false) BigDecimal amount,
                                   @RequestParam(required = false) String amountFilter, // "=", "<=", ">="
                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                   @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                   Model model, HttpServletRequest request) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionService.findTransactions(description, amount, amountFilter, startDate, endDate, pageable);
        model.addAttribute("requestURI", request.getRequestURI());
        model.addAttribute("transactions", transactions);
        model.addAttribute("description", description);
        model.addAttribute("amount", amount);
        model.addAttribute("amountFilter", amountFilter);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "transactions"; // View that displays the list of transactions
    }

    // Method to show the form to add a new transaction
    @GetMapping("/add")
    public String showTransactionForm(Model model, HttpServletRequest request) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories); // Pass categories to the form
        model.addAttribute("requestURI", request.getRequestURI());
        model.addAttribute("transaction", new Transaction());
        model.addAttribute("pageTitle", "Add New Transaction - Expense Tracker");
        return "add-transaction";
    }

    // Method to save the new transaction
    @PostMapping("/add")
    public String saveTransaction(@Valid @ModelAttribute("transaction") Transaction transaction,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "add-transaction";
        }
        transactionRepository.save(transaction);
        redirectAttributes.addFlashAttribute("successMessage", "Transaction added successfully!");
        return "redirect:/transactions"; // Redirect to  transaction list
    }

    @GetMapping("/view/{id}")
    @ResponseBody
    public ResponseEntity<Transaction> viewTransaction(@PathVariable Long id) {
        // Fetch the transaction by ID from the repository or service layer
        Transaction transaction = transactionService.getTransactionById(id);

        // Check if transaction exists and return JSON response
        if (transaction != null) {
            return ResponseEntity.ok(transaction);
        } else {
            return ResponseEntity.notFound().build(); // Return 404 if transaction not found
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, HttpServletRequest request) {
        Transaction transaction = transactionService.getTransactionById(id);
        model.addAttribute("requestURI", request.getRequestURI());
        if (transaction != null) {
            List<Category> categories = categoryService.getAllCategories();
            model.addAttribute("categories", categories); // Pass categories to the form
            model.addAttribute("transaction", transaction);

            model.addAttribute("pageTitle", "Edit Transaction - Expense Tracker");
            return "edit-transaction"; // The edit page template
        } else {
            return "redirect:/transactions"; // Redirect to list if transaction not found
        }
    }

    @PostMapping("/edit/{id}")
    public String updateTransaction(@PathVariable Long id,
                                    @Valid @ModelAttribute("transaction") Transaction transaction,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes, HttpServletRequest request) {
        if (result.hasErrors()) {
            return "edit-transaction"; // Return to the form if there are validation errors
        }

        // Set the transaction ID to ensure we are updating the correct record
        transaction.setId(id);
        transactionRepository.save(transaction); // Save the updated transaction

        redirectAttributes.addFlashAttribute("successMessage", "Transaction updated successfully!");
        return "redirect:/transactions"; // Redirect back to transaction list
    }





}
