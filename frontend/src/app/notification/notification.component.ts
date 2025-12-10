import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { NotificationService } from '../services/notification.service';

@Component({
  selector: 'app-notification',
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.scss']
})
export class NotificationComponent implements OnInit, OnDestroy {
  notifications: any[] = [];
  unreadCount = 0;
  showNotifications = false;
  private notificationSubscription: Subscription;

  constructor(private notificationService: NotificationService) { }

  ngOnInit(): void {
    // S'abonner aux notifications en temps réel
    this.notificationSubscription = this.notificationService.notifications$.subscribe(notification => {
      if (notification.type === 'NEW_NOTIFICATIONS') {
        this.notifications = [...notification.notifications, ...this.notifications];
        this.unreadCount += notification.count;
      } else {
        // Notification simple
        this.notifications = [notification, ...this.notifications];
        this.unreadCount++;
      }
    });
  }

  ngOnDestroy(): void {
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
  }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
  }

  markAsRead(notification: any): void {
    this.notificationService.markAsRead(notification.id).subscribe(() => {
      notification.read = true;
      this.unreadCount = Math.max(0, this.unreadCount - 1);
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe(() => {
      this.notifications.forEach(n => n.read = true);
      this.unreadCount = 0;
    });
  }

  clearNotifications(): void {
    this.notifications = [];
    this.unreadCount = 0;
    this.showNotifications = false;
  }
}
