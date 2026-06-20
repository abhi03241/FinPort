<div align="center">
  <h1>📈 FinPort: Personal Financial Portfolio Manager 📈</h1>
<h3>
  ✨ <a href="http://13.201.204.129:8080/index">Live Project</a>
</h3>
</div>

<!-- Table of Contents -->

# 📓 Table of Contents

* [About the Project](#star2-about-the-project)

  * [Screenshots](#camera-screenshots)
  * [Technologies Used](#space_invader-technologies-used)
  * [Features](#dart-features)
  * [Environment Variables](#key-environment-variables)
* [Getting Started](#toolbox-getting-started)

  * [Prerequisites](#bangbang-prerequisites)
  * [Installation](#gear-installation)
* [Deployment](#triangular_flag_on_post-deployment)
* [Usage](#eyes-usage)
* [Contributing](#wave-contributing)
* [License](#warning-license)
* [Contact](#handshake-contact)

## ⭐ About the Project

FinPort is a comprehensive and intelligent personal financial portfolio manager web application built using Spring Boot and Thymeleaf. It allows users to manage their income, expenses, savings, EMIs, loans, taxes, and more—all in one place. It simplifies finance tracking with powerful visual insights and reminders.

### 📷 Screenshots

<div align="center">
  <table>
    <tr>
      <td><img src="https://res.cloudinary.com/divq45mjo/image/upload/v1747567912/Screenshot_2025-05-13_205606_vtbjep.png" alt="screenshot" width="400"/></td>
      <td><img src="https://res.cloudinary.com/divq45mjo/image/upload/v1747567913/Screenshot_2025-05-18_170010_gq2vrr.png" alt="screenshot" width="400"/></td>
    </tr>
    <tr>
      <td><img src="https://res.cloudinary.com/divq45mjo/image/upload/v1747567913/Screenshot_2025-05-18_170023_wuqig1.png" alt="screenshot" width="400"/></td>
      <td><img src="https://res.cloudinary.com/divq45mjo/image/upload/v1747567913/Screenshot_2025-05-18_170049_kyr5ce.png" alt="screenshot" width="400"/></td>
    </tr>
    <tr>
      <td><img src="https://res.cloudinary.com/divq45mjo/image/upload/v1747567913/Screenshot_2025-05-18_170058_abajer.png" alt="screenshot" width="400"/></td>
      <td><img src="https://res.cloudinary.com/divq45mjo/image/upload/v1747567913/Screenshot_2025-05-18_170107_mqmfiy.png" alt="screenshot" width="400"/></td>
    </tr>
  </table>
</div>

  
  
</div>



### 👾 Technologies Used

<details>
  <summary>Backend</summary>
  <ul>
    <li>Java 17</li>
    <li>Spring Boot 3.2.5</li>
    <li>Spring Data JPA (Hibernate)</li>
  </ul>
</details>

<details>
  <summary>Frontend</summary>
  <ul>
    <li>HTML5, CSS3, JavaScript</li>
    <li>Thymeleaf</li>
  </ul>
</details>

<details>
  <summary>Database</summary>
  <ul>
    <li>MySQL</li>
  </ul>
</details>

<details>
  <summary>Deployment</summary>
  <ul>
    <li>AWS EC2</li>
  </ul>
</details>

### 🎯 Features

* ✉️ **User Registration/Login**
* 💼 **Expense & Income Management**
* 💳 **EMI Tracking and Alerts**
* 💵 **Loan & Udhaar Management**
* 🎁 **Savings Goals Setup**
* 📆 **Reminder System for Bills & Payments**

### 🔑 Environment Variables

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/finport_db
spring.datasource.username=your_username
spring.datasource.password=your_password
openai.api.key=your_key
openai.api.url=https://api.openai.com/v1/chat/completions
```

## 🛠️ Getting Started

### ‼️ Prerequisites

* Java 17+
* Maven
* MySQL

### ⚙️ Installation

```bash
git clone https://github.com/abhi03241/FinPort.git
cd FinPort
```

Update `application.properties` with your MySQL credentials, then:

```bash
mvn spring-boot:run
```

Visit: `http://localhost:8080/index`

## 📍 Deployment

The application is deployed on an **AWS EC2** instance.

```bash
# For running in background
nohup java -jar target/FinPort-0.0.1-SNAPSHOT.jar &
```

Access Live at: [http://13.201.204.129:8080/index](http://13.201.204.129:8080/index)

## 👁️ Usage

* Register or login as a user
* Navigate through Dashboard, Expenses, Income, EMIs, Goals, and Reminders
* Use built-in charts and summaries to track financial health

## 👋 Contributing

We welcome contributions! Create a pull request or raise issues.

## ⚠️ License

This project is licensed under the MIT License.

## 🤝 Contact

**Abhishek Shukla**

GitHub: [https://github.com/abhi03241](https://github.com/abhi03241)
