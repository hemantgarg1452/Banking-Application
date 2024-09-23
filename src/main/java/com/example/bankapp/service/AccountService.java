package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.model.Transaction;
import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class AccountService implements UserDetailsService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // find account by username....
    public Account findAccountByUsername(String username){
        return accountRepository.findByUsername(username).orElseThrow(()->new RuntimeException("Account Not Found!"));
    }

    // Register Account....
    public Account registerAccount(String username, String password){
        if(accountRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("User already exists!");
        }

        Account account = new Account();
        account.setUserName(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setBalance(BigDecimal.ZERO);
        return accountRepository.save(account);
    }

    // Deposit Money into account....
    public void Deposit(Account account, BigDecimal amount){
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                amount,
                "Deposit",
                LocalDateTime.now(),
                account
        );
        transactionRepository.save(transaction);
    }

    // Withdraw Money from account....
    public void Withdraw(Account account, BigDecimal amount){
        if(account.getBalance().compareTo(amount)<0){
            throw new RuntimeException("Insufficient Balance");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                amount,
                "Withdraw",
                LocalDateTime.now(),
                account
        );
        transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionHistory(Account account){
        return transactionRepository.findByAccountID(account.getId());
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = findAccountByUsername(username);
        if(account==null){
            throw new UsernameNotFoundException("User not found");
        }
        return new Account(
                account.getUserName(),
                account.getPassword(),
                account.getBalance(),
                account.getTransactions(),
                authorities()
        );
    }

    private Collection<? extends GrantedAuthority> authorities() {
        return Arrays.asList(new SimpleGrantedAuthority("user"));
    }

    // Transfer the money...
    public void transferMoney(Account fromAccount, String username, BigDecimal amount){
        if(fromAccount.getBalance().compareTo(amount)<0){
            throw new RuntimeException("Insufficient Balance!");
        }

        Account toAccount = accountRepository.findByUsername(username)
                .orElseThrow(()->new RuntimeException("Account not found!"));

        // Deduct the Money...
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepository.save(fromAccount);

        // Add in other account...
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        // Record the Transactions...
        Transaction debitedTransactions = new Transaction(
                amount,
                "Transfer out to"+toAccount.getUserName(),
                LocalDateTime.now(),
                fromAccount
        );
        transactionRepository.save(debitedTransactions);

        Transaction creditTransactions = new Transaction(
                amount,
                "Transfer in from"+fromAccount.getUserName(),
                LocalDateTime.now(),
                toAccount
        );
        transactionRepository.save(creditTransactions);
    }
}
