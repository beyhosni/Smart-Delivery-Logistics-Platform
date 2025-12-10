import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { DeliveryListComponent } from './delivery/delivery-list/delivery-list.component';
import { DeliveryDetailComponent } from './delivery/delivery-detail/delivery-detail.component';
import { CreateDeliveryComponent } from './delivery/create-delivery/create-delivery.component';
import { TrackingComponent } from './tracking/tracking.component';
import { ProfileComponent } from './profile/profile.component';
import { AuthGuard } from './guards/auth.guard';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { 
    path: 'dashboard', 
    component: DashboardComponent,
    canActivate: [AuthGuard]
  },
  { 
    path: 'deliveries', 
    component: DeliveryListComponent,
    canActivate: [AuthGuard]
  },
  { 
    path: 'deliveries/new', 
    component: CreateDeliveryComponent,
    canActivate: [AuthGuard]
  },
  { 
    path: 'deliveries/:id', 
    component: DeliveryDetailComponent,
    canActivate: [AuthGuard]
  },
  { 
    path: 'tracking/:id', 
    component: TrackingComponent,
    canActivate: [AuthGuard]
  },
  { 
    path: 'profile', 
    component: ProfileComponent,
    canActivate: [AuthGuard]
  },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
