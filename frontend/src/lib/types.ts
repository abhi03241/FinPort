export interface Category {
  id: number;
  name: string;
  createdAt?: string;
  updatedAt?: string;
}

export type TransactionType = "INCOME" | "EXPENSE";

export interface Transaction {
  id: number;
  amount: number;
  date: string; // ISO yyyy-MM-dd
  description: string;
  transactionType: TransactionType;
  category: Category;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}